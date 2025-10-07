package io.github.egorkor.webutils.queryparam;

import io.github.egorkor.webutils.annotations.AllowedOperations;
import io.github.egorkor.webutils.annotations.FieldParamMapping;
import io.github.egorkor.webutils.annotations.ParamCountLimit;
import io.github.egorkor.webutils.exception.InvalidParameterException;
import io.github.egorkor.webutils.queryparam.filterInternal.*;
import io.github.egorkor.webutils.queryparam.utils.FieldTypeUtils;
import jakarta.persistence.criteria.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.egorkor.webutils.annotations.FieldParamMapping.NO_MAPPING;
import static io.github.egorkor.webutils.queryparam.filterInternal.FilterOperation.IS;

/**
 * Параметр запроса для фильтрации запрашиваемых ресурсов.
 *
 * @author EgorKor
 * @version 1.0.4
 * @since 2025
 */
//TODO: добавить поддержку операций работы с JSON
//TODO: добавить поддержку функций size() length() для SQL
//TODO: реализовать метод обновления по фильтру
@Slf4j
@Setter
@Getter
public class Filter<T> implements Specification<T> {
    public final static FilterBuilder fb = new FilterBuilder();
    public static final Pattern FUNCTION_PATTERN = Pattern.compile("(.*)\\.(length\\(\\)|size\\(\\))");
    public static final Pattern CONCAT_FUNCTION_PATTERN = Pattern.compile("concat\\((.*)\\)");
    public static final Pattern TO_CHAR_FUNCTION_PATTERN = Pattern.compile("to_char\\((.*);'(.*)'\\)");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .toFormatter();
    protected List<FilterBasicOperation> operations;
    protected Class<?> entityType;
    protected List<Consumer<Root<T>>> queryConfigurers = new ArrayList<>();
    private List<String> fieldWhiteList = new ArrayList<>();
    private List<String> fetchingProperties = new ArrayList<>();

    public Filter() {
        this.operations = new ArrayList<>();
        determineEntityType();
    }

    public Filter(List<FilterBasicOperation> operations) {
        this.operations = new ArrayList<>(operations);
        determineEntityType();
    }

    public Filter(Class<T> entityType) {
        this.entityType = entityType;
        this.operations = new ArrayList<>();
    }

    public Filter(List<FilterBasicOperation> operations, Class<?> entityType) {
        this.operations = operations;
        this.entityType = entityType;
    }

    @SneakyThrows
    public <R extends Filter<?>> R copy() {
        R copiedFilter = (R) this.getClass().getDeclaredConstructor().newInstance();
        copiedFilter.setEntityType(entityType);
        copiedFilter.setFieldWhiteList(fieldWhiteList);
        copiedFilter.setOperations(operations);
        return copiedFilter;
    }

    public static <T> Path<T> getNestedPath(Root<T> root, String field) {
        String[] fields = field.split("\\.");
        Path<T> path = root.get(fields[0]);
        for (int i = 1; i < fields.length; i++) {
            path = path.get(fields[i]);
        }
        return path;
    }

    public static FilterBuilder builder() {
        return fb;
    }

    public static <T extends Filter<?>> T softDeleteFilter(Field field, boolean isDeleted) {
        return softDeleteFilter(field.getName(), field.getType(), isDeleted);
    }

    public static <T extends Filter<?>> T softDeleteFilter(Field field, boolean isDeleted, Class<T> entityType) {
        T softDeleteFilter = softDeleteFilter(field.getName(), field.getType(), isDeleted);
        softDeleteFilter.setEntityType(entityType);
        return softDeleteFilter;
    }

    public static <T extends Filter<?>> T softDeleteFilter(String fieldName, Class<?> fieldType, boolean isDeleted) {
        T filter = (T) new Filter<>();
        List<FilterBasicOperation> filterList = new ArrayList<>();
        if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
            filterList.add(new FilterBasicOperation(fieldName, IS, isDeleted));
        } else {
            filterList.add(new FilterBasicOperation(fieldName, IS, isDeleted ? Is.NOT_NULL : Is.NULL));
        }
        filter.setOperations(filterList);
        return filter;
    }

    public static <T> Filter<T> empty() {
        return new Filter<>();
    }

    public static <T> Filter<T> empty(Class<T> entityType) {
        return new Filter<>(entityType);
    }

    private static Predicate getComparisonPredicate(CriteriaBuilder cb,
                                                    FilterOperation operation,
                                                    Expression<Comparable> comparableSelection,
                                                    Comparable value) {
        return switch (operation) {
            case GT -> cb.greaterThan(comparableSelection, value);
            case LS -> cb.lessThan(comparableSelection, value);
            case GTE -> cb.greaterThanOrEqualTo(comparableSelection, value);
            case LSE -> cb.lessThanOrEqualTo(comparableSelection, value);
            default -> throw new InvalidParameterException("Некорректное операция сравнения: " + operation);
        };
    }

    public static <X> Expression<X> getTypedExpression(Expression<?> expression, Class<X> type) {
        return (Expression<X>) expression;
    }

    public boolean isFiltered() {
        return !operations.isEmpty();
    }

    public boolean isUnfiltered() {
        return operations.isEmpty();
    }


    //region Criteria API Mapping
    @Override
    public Predicate toPredicate(Root<T> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        return toPredicate(root, cb);
    }

    public Predicate toPredicate(Root<T> root,
                                 CriteriaBuilder cb) {
        if (queryConfigurers.isEmpty()) {
            configureQuery(root);
        } else {
            queryConfigurers.forEach(c -> c.accept(root));
        }
        Map<String, List<Predicate>> predicates = new HashMap<>();
        operations.forEach(f -> {
            Predicate predicate = parsePredicate(f, root, cb);
            if (predicates.containsKey(f.field())) {
                predicates.get(f.field()).add(predicate);
            } else {
                predicates.put(f.field(), new ArrayList<>(List.of(predicate)));
            }
        });
        return collectPredicates(cb, predicates);
    }

    /**
     * Предназначен для переопределения,
     * например чтобы
     */
    protected void configureQuery(Root<T> root) {
    }

    public <R extends Filter<?>> R configureQuery(Consumer<Root<T>> queryConfigurer) {
        queryConfigurers.add(queryConfigurer);
        return _this();
    }

    public <R extends Filter<?>> R _and(Filter<?> filter) {
        this.initializeOriginalNamesMap();
        this.operations.addAll(filter.getOperations());
        this.fieldWhiteList.addAll(
                filter.getOperations()
                        .stream()
                        .map(FilterBasicOperation::field)
                        .toList());
        if (filter.originalNames != null) {
            filter.originalNames.forEach(
                    (field, filters) -> {
                        if (this.originalNames.containsKey(field)) {
                            this.originalNames.get(field).addAll(filters);
                        } else {
                            this.originalNames.put(field, filters);
                        }
                    }
            );
        }
        return _this();
    }

    public <R extends Filter<?>> R withFetchJoin(String fetchingProperty) {
        this.fetchingProperties.add(fetchingProperty);
        queryConfigurers.add((root) -> {
            String[] attributes = fetchingProperty.split("\\.");
            FetchParent<?, ?> currentParent = root;

            for (String attribute : attributes) {
                currentParent = currentParent.fetch(attribute, JoinType.LEFT);
            }
        });
        return _this();
    }

    public static <T> Filter<T> equal(String field, Object value) {
        return fb.and(fb.equals(field, value));
    }

    public static <T> Filter<T> notEqual(String field, Object value) {
        return fb.and(fb.notEquals(field, value));
    }

    public static <T> Filter<T> contains(String field, String value) {
        return fb.and(fb.like(field, value));
    }

    public static <T> Filter<T> notContains(String field, String value) {
        return fb.and(fb.notLike(field, value));
    }

    public static <T> Filter<T> like(String field, String value) {
        return fb.and(fb.like(field, value));
    }

    public static <T> Filter<T> notLike(String field, String value) {
        return fb.and(fb.notLike(field, value));
    }

    public static <T> Filter<T> is(String field, Is value) {
        return fb.and(fb.is(field, value));
    }

    public static <T> Filter<T> in(String field, Object... values) {
        return fb.and(fb.in(field, values));
    }

    public static <T> Filter<T> inCollection(String field, Collection<?> values) {
        return fb.and(fb.inCollection(field, values));
    }

    public static <T> Filter<T> notIn(String field, Object... values) {
        return fb.and(fb.notIn(field, values));
    }

    public static <T> Filter<T> notInCollection(String field, Collection<?> values) {
        return fb.and(fb.notInCollection(field, values));
    }

    public static <T> Filter<T> greaterThan(String field, Comparable<?> value) {
        return fb.and(fb.greater(field, value));
    }

    public static <T> Filter<T> greaterThanOrEqual(String field, Comparable<?> value) {
        return fb.and(fb.greaterOrEquals(field, value));
    }

    public static <T> Filter<T> lessThan(String field, Comparable<?> value) {
        return fb.and(fb.less(field, value));
    }

    public static <T> Filter<T> lessThanOrEqual(String field, Comparable<?> value) {
        return fb.and(fb.lessOrEquals(field, value));
    }


    protected Predicate collectPredicates(CriteriaBuilder cb,
                                          Map<String, List<Predicate>> predicates) {
        return cb.and(predicates.values().stream()
                .flatMap(Collection::stream)
                .toList().toArray(new Predicate[0]));
    }

    private Predicate parsePredicate(FilterBasicOperation filter,
                                     Root<T> root,
                                     CriteriaBuilder cb) {


        String field = filter.field();
        Object value = filter.value();
        FilterOperation operation = filter.operation();

        Function function = null;
        Matcher functionMatcher = FUNCTION_PATTERN.matcher(field);
        if (functionMatcher.matches()) {
            String functionStr = functionMatcher.group(2);
            function = Function.parseByOperation(functionStr);
            field = field.substring(0, field.lastIndexOf(functionStr) - 1);
        }

        Expression<?> selection = getSelectExpression(root, field, cb);
        Field reflectionField = FieldTypeUtils.getField(entityType, field);
        try {

            return switch (operation) {
                case IS -> parseIsPredicate(cb, selection, Is.parse(value.toString()));
                case IS_NOT -> cb.not(parseIsPredicate(cb, selection, Is.parse(value.toString())));
                case EQUALS -> parseEqualPredicate(cb, selection, reflectionField, value, function);
                case GT, LS, GTE, LSE ->
                        parseComparisonPredicate(cb, selection, operation, reflectionField, value, function);
                case NOT_EQUALS -> cb.not(parseEqualPredicate(cb, selection, reflectionField, value, function));
                case CONTAINS -> parseContainsPredicate(cb, selection, value.toString());
                case NOT_CONTAINS -> cb.not(parseContainsPredicate(cb, selection, value.toString()));
                case LIKE -> parseLikePredicate(cb, selection, value.toString());
                case NOT_LIKE -> cb.not(parseLikePredicate(cb, selection, value.toString()));
                case IN -> parseInPredicate(cb, selection, reflectionField, (Collection<?>) value, function);
                case NOT_IN -> cb.not(parseInPredicate(cb, selection, reflectionField, (List<Object>) value, function));
            };
        } catch (Exception e) {
            throw new InvalidParameterException(
                    String.format("Ошибка обработки фильтра '%s' для поля '%s': %s",
                            filter, field, e.getMessage()), e);
        }
    }

    private static Class<?> getFieldType(Field reflectionField, Function function) {
        if (function != null) {
            if (function == Function.LENGTH || function == Function.SIZE) {
                return Long.class;
            }
        }
        return reflectionField != null ? reflectionField.getType() : null;
    }

    private static <T> Expression<?> getSelectExpression(Root<T> root, String field, CriteriaBuilder cb) {
        if (!field.startsWith("concat")) {
            return field.contains(".") ? getNestedPath(root, field) : root.get(field);
        }
        Matcher concatMatcher = CONCAT_FUNCTION_PATTERN.matcher(field);
        if (!concatMatcher.matches()) {
            throw new InvalidParameterException("Невалидный синтаксис операции concat: " + field);
        }
        String[] concatValues = concatMatcher.group(1).split(",");
        List<Expression<?>> concatExpressions = new ArrayList<>();
        for (String concatValue : concatValues) {

            String trimmedValue = concatValue.trim();
            //string literal case
            if (concatValue.startsWith("'") && concatValue.endsWith("'")) {
                String literalValue = trimmedValue.substring(1, trimmedValue.length() - 1);
                concatExpressions.add(cb.literal(literalValue));
                continue;
            }

            Matcher toCharMatcher = TO_CHAR_FUNCTION_PATTERN.matcher(trimmedValue);
            if (toCharMatcher.matches()) {
                String path = toCharMatcher.group(1);
                String format = toCharMatcher.group(2);
                concatExpressions.add(cb.function(
                        "TO_CHAR",
                        String.class,
                        getSelectExpression(root, path, cb),
                        cb.literal(format)
                ));
            } else {
                concatExpressions.add(getSelectExpression(root, trimmedValue, cb));
            }

        }

        return concateExpressions(cb, concatExpressions);
    }

    private static Expression<String> concateExpressions(CriteriaBuilder cb, List<Expression<?>> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            return cb.literal("");
        }

        Expression<String> result = null;

        for (Expression<?> expr : expressions) {
            if (result == null) {
                result = convertToString(cb, expr);
            } else {
                result = cb.concat(result, convertToString(cb, expr));
            }
        }

        return result;
    }

    private static Expression<String> convertToString(CriteriaBuilder cb, Expression<?> expression) {
        if (expression.getJavaType() == String.class) {
            return (Expression<String>) expression;
        }
        // Для числовых и других типов преобразуем в строку
        return cb.toString((Expression<Character>) expression);
    }

    private Predicate parseInPredicate(CriteriaBuilder cb,
                                       Expression<?> selection,
                                       Field reflectionField,
                                       Collection<?> inValues,
                                       Function function) {

        //Если есть функция size или length

        if (reflectionField != null && Collection.class.isAssignableFrom(reflectionField.getType())) {
            Class<?> elementType = getCollectionElementType(reflectionField);
            if (function != null) {
                Object[] values = inValues
                        .stream()
                        .map(v -> convertValue(v, elementType))
                        .toArray();
                return getFunctionPath(cb, selection, function).in(values);
            }


            List<Predicate> predicates = new ArrayList<>();
            for (Object inValue : inValues) {
                Object val = convertValue(inValue, elementType);
                predicates.add(cb.isMember(val, (Path<Collection>) selection));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        }
        // Для обычных полей
        Class<?> fieldType = getFieldType(reflectionField, function);
        Object[] values = inValues.stream()
                .map(v -> convertValue(v, fieldType))
                .toArray();
        return selection.in(values);
    }

    public static Class<?> getCollectionElementType(Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        return String.class;
    }

    @SneakyThrows
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (value.getClass().equals(targetType)) {
            return value;
        }

        // Конвертация между числовыми типами
        if (Number.class.isAssignableFrom(targetType) && value instanceof Number number) {

            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return number.intValue();
            } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return number.longValue();
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return number.doubleValue();
            } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return number.floatValue();
            } else if (targetType.equals(Short.class) || targetType.equals(short.class)) {
                return number.shortValue();
            } else if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
                return number.byteValue();
            } else if (targetType.equals(BigDecimal.class)) {
                return new BigDecimal(number.toString());
            } else if (targetType.equals(BigInteger.class)) {
                return BigInteger.valueOf(number.longValue());
            }
        }

        // Конвертация строк в числа
        if (Number.class.isAssignableFrom(targetType) && value instanceof String) {
            String stringValue = ((String) value).trim();

            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return Integer.parseInt(stringValue);
            } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return Long.parseLong(stringValue);
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return Double.parseDouble(stringValue);
            } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return Float.parseFloat(stringValue);
            } else if (targetType.equals(Short.class) || targetType.equals(short.class)) {
                return Short.parseShort(stringValue);
            } else if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
                return Byte.parseByte(stringValue);
            } else if (targetType.equals(BigDecimal.class)) {
                return new BigDecimal(stringValue);
            } else if (targetType.equals(BigInteger.class)) {
                return new BigInteger(stringValue);
            }
        }

        if (value instanceof Is is) {
            return switch (is) {
                case TRUE -> true;
                case FALSE -> false;
                case NULL -> null;
                default -> throw new InvalidParameterException("Некорректное значение для операции is: " + value);
            };
        }

        if (value.getClass() != String.class) {
            throw new InvalidParameterException("Невозможно преобразовать объект типа %s в тип %s"
                    .formatted(value.getClass().getSimpleName(), targetType.getSimpleName()));
        }
        String stringValue = value.toString();
        try {
            if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(stringValue);
            if (targetType == Long.class || targetType == long.class) return Long.parseLong(stringValue);
            if (targetType == Double.class || targetType == double.class) return Double.parseDouble(stringValue);
            if (targetType == Float.class || targetType == float.class) return Float.parseFloat(stringValue);
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(stringValue);
            if (targetType == java.sql.Date.class)
                return java.sql.Date.valueOf(LocalDate.parse(stringValue, DATE_TIME_FORMATTER));
            if (targetType == LocalDate.class) return LocalDate.parse(stringValue, DATE_TIME_FORMATTER);
            if (targetType == LocalDateTime.class) {
                try {
                    return LocalDateTime.parse(stringValue, DATE_TIME_FORMATTER);
                } catch (Exception e) {
                    return LocalDateTime.parse(stringValue);
                }
            }
            if (targetType.isEnum()) return Enum.valueOf((Class<Enum>) targetType, stringValue);

            throw new InvalidParameterException(": " + targetType.getName());
        } catch (Exception e) {
            throw new InvalidParameterException(
                    String.format("Невозможно преобразовать '%s в %s: %s",
                            stringValue, targetType.getSimpleName(), e.getMessage()), e);
        }
    }

    private Predicate parseIsPredicate(CriteriaBuilder cb, Expression<?> selection, Is value) {
        return switch (value) {
            case TRUE -> cb.isTrue(getTypedExpression(selection, Boolean.class));
            case FALSE -> cb.isFalse(getTypedExpression(selection, Boolean.class));
            case NULL -> cb.isNull(selection);
            case NOT_NULL -> cb.isNotNull(selection);
        };
    }

    private Predicate parseEqualPredicate(CriteriaBuilder cb,
                                          Expression<?> selection,
                                          Field reflectionField,
                                          Object value,
                                          Function function) {
        if (reflectionField != null && Collection.class.isAssignableFrom(reflectionField.getType())) {

            if (function != null) {
                return switch (function) {
                    case LENGTH, SIZE -> cb.equal(getFunctionPath(cb, selection, function),
                            convertValue(value, Long.class));
                };
            }
            Object convertedValue = convertValue(value, getCollectionElementType(reflectionField));
            return cb.isMember(convertedValue, (Expression<Collection>) selection);
        }
        value = convertValue(value, getFieldType(reflectionField, function));
        return cb.equal(getFunctionPath(cb, selection, function), value);
    }

    private Expression<?> getFunctionPath(CriteriaBuilder cb, Expression<?> current, Function function) {
        if (function == null) {
            return current;
        }
        return switch (function) {
            case LENGTH -> cb.length(getTypedExpression(current, String.class));
            case SIZE -> cb.size(getTypedExpression(current, Collection.class));
        };

    }

    private Predicate parseComparisonPredicate(CriteriaBuilder cb,
                                               Expression<?> selection,
                                               FilterOperation operation,
                                               Field reflectionField,
                                               Object value,
                                               Function function) {
        if (reflectionField != null && !Comparable.class.isAssignableFrom(reflectionField.getType())
                && function == null) {
            throw new InvalidParameterException("Аттрибут выборки " + selection + " не реализует интерфейс Comparable");
        }

        Expression<Comparable> comparablePath = (Expression<Comparable>) getFunctionPath(cb, selection, function);

        if (reflectionField != null && Collection.class.isAssignableFrom(reflectionField.getType())) {
            if (function != null) {
                return switch (function) {
                    case LENGTH, SIZE ->
                            getComparisonPredicate(cb, operation, comparablePath, (Long) convertValue(value, Long.class));
                };
            }

            Comparable<?> convertedValue = (Comparable<?>) convertValue(value, getCollectionElementType(reflectionField));
            return cb.isMember(convertedValue, (Path<Collection>) selection);
        }

        Class<?> type = getFieldType(reflectionField, function);
        Comparable<?> comparableValue = (Comparable<?>) convertValue(value, type);
        return getComparisonPredicate(cb, operation, comparablePath, comparableValue);
    }

    //endregion


    //region Utility Methods

    public boolean isParameterPresent(String paramName) {
        initializeOriginalNamesMap();
        return originalNames.containsKey(paramName);
    }

    public FilterBasicOperation getFirst(String paramName) {
        initializeOriginalNamesMap();
        return originalNames.get(paramName)
                .stream()
                .findFirst()
                .get();
    }

    public Set<FilterBasicOperation> get(String paramName) {
        return originalNames.get(paramName);
    }

    private Map<String, Set<FilterBasicOperation>> fieldFiltersIndex() {
        Map<String, Set<FilterBasicOperation>> index = new HashMap<>();
        for (var operation : operations) {
            if (index.containsKey(operation.field())) {
                index.get(operation.field()).add(operation);
            } else {
                index.put(operation.field(), new HashSet<>(Set.of(operation)));
            }
        }
        return index;
    }

    private Map<String, Set<FilterOperation>> fieldOperationIndex() {
        Map<String, Set<FilterOperation>> index = new HashMap<>();
        for (var operation : operations) {
            if (index.containsKey(operation.field())) {
                index.get(operation.field()).add(operation.operation());
            } else {
                index.put(operation.field(), new HashSet<>(Set.of(operation.operation())));
            }
        }
        return index;
    }

    private Predicate parseContainsPredicate(CriteriaBuilder cb, Expression<?> selection, String stringValue) {
        Expression<String> stringSelection = cb.lower(getTypedExpression(selection, String.class));
        return cb.like(stringSelection, "%" + stringValue.toLowerCase() + "%");
    }

    private Predicate parseLikePredicate(CriteriaBuilder cb, Expression<?> selection, String stringValue) {
        Expression<String> stringPath = getTypedExpression(selection, String.class);
        return cb.like(stringPath, stringValue);
    }

    private void determineEntityType() {
        if (getClass() == Filter.class) {
            return;
        }
        try {
            Type superclass = getClass().getGenericSuperclass();
            ParameterizedType parameterizedType = (ParameterizedType) superclass;
            Type typeArgument = parameterizedType.getActualTypeArguments()[0];
            this.entityType = typeArgument.getClass();
        } catch (Exception e) {
            log.warn("Cannot determine entity type", e);
        }
    }

    private <SameType extends Filter<?>> SameType _this() {
        return (SameType) this;
    }

    private Map<String, Set<FilterBasicOperation>> originalNames;


    public void mapFilterByAllies() {
        if (this.getClass() == Filter.class) {
            return;
        }
        initializeOriginalNamesMap();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            FieldParamMapping fieldParamMapping = field.getAnnotation(FieldParamMapping.class);
            if (fieldParamMapping == null
                    || fieldParamMapping.sqlMapping().equals(NO_MAPPING)) {
                continue;
            }
            String alliesName = fieldParamMapping.sqlMapping();
            String fieldName = Objects.equals(fieldParamMapping.requestParamMapping(), NO_MAPPING)
                    ? field.getName() : fieldParamMapping.requestParamMapping();
            String regexSafeFieldName = Pattern.quote(fieldName);

            for (int i = 0; i < operations.size(); i++) {
                FilterBasicOperation op = operations.get(i);

                if (fieldName.equals(op.field())) {
                    operations.set(i, new FilterBasicOperation(
                            op.field().replaceFirst(regexSafeFieldName, alliesName),
                            op.operation(),
                            op.value()));
                }

            }
        }
    }

    public void checkAllowedFilterFields() {
        if (this.getClass() == Filter.class) {
            return;
        }
        ParamCountLimit limit;
        if ((limit = this.getClass().getAnnotation(ParamCountLimit.class)) != null
                && limit.value() != ParamCountLimit.UNLIMITED
                && operations.size() > limit.value()) {
            throw new InvalidParameterException("Недопустимое общее кол-во фильтров: " + operations.size()
                    + ". Допустимое значение: " + limit.value());
        }
        initializeOriginalNamesMap();
        Set<String> paramsNames = originalNames.keySet();

        Field[] declaredFields = this.getClass().getDeclaredFields();
        Set<String> allowedFields = Arrays.stream(declaredFields)
                .map(f -> {


                    FieldParamMapping allies;
                    String paramName;
                    if ((allies = f.getAnnotation(FieldParamMapping.class)) != null
                            && !Objects.equals(allies.requestParamMapping(), NO_MAPPING)) {
                        paramName = allies.requestParamMapping();
                    } else {
                        paramName = f.getName();
                    }

                    ParamCountLimit paramLimit = f.getAnnotation(ParamCountLimit.class);
                    if (paramLimit != null && isParameterPresent(paramName)
                            && get(paramName).size() > paramLimit.value()) {
                        throw new InvalidParameterException("Недопустимое кол-во фильтров для параметра %s: "
                                .formatted(paramName) + operations.size() + ". Допустимое значение: " + paramLimit.value());
                    }


                    return paramName;
                })
                .collect(Collectors.toSet());

        paramsNames.removeAll(allowedFields);
        fieldWhiteList.forEach(paramsNames::remove);
        if (!paramsNames.isEmpty()) {
            throw new InvalidParameterException("Недопустимые параметры фильтрации: " + paramsNames);
        }
    }

    private void initializeOriginalNamesMap() {
        if (originalNames == null) {
            originalNames = fieldFiltersIndex();
        }
    }

    public void checkAllowedFilterOperations() {
        if (this.getClass() == Filter.class) {
            return;
        }
        Map<String, Set<FilterOperation>> index = fieldOperationIndex();
        Field[] fields = this.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (!field.isAnnotationPresent(AllowedOperations.class)) {
                continue;
            }

            String originalName = field.getName();
            String checkingName = field.getName();
            if (field.isAnnotationPresent(FieldParamMapping.class)) {
                FieldParamMapping fieldParamMapping = field.getAnnotation(FieldParamMapping.class);
                if (!fieldParamMapping.sqlMapping().equals(NO_MAPPING)) {
                    checkingName = fieldParamMapping.sqlMapping();
                }
                if (!fieldParamMapping.requestParamMapping().equals(NO_MAPPING)) {
                    originalName = fieldParamMapping.requestParamMapping();
                }
            }

            AllowedOperations allowedOperationsAnnotation = field.getAnnotation(AllowedOperations.class);

            if (index.containsKey(checkingName)) {
                Set<FilterOperation> usedOperations = index.get(checkingName);
                Set<FilterOperation> allowedOperations = Arrays.stream(allowedOperationsAnnotation.value())
                        .collect(Collectors.toSet());

                for (FilterOperation usedOp : usedOperations) {
                    if (!allowedOperations.contains(usedOp)) {
                        throw new InvalidParameterException("Недопустимая операция " + usedOp + " для параметра " + originalName);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Filter = AND" + operations;
    }

    //endregion


}
