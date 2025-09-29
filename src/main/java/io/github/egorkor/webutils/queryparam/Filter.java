package io.github.egorkor.webutils.queryparam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.egorkor.webutils.annotations.FieldParamMapping;
import io.github.egorkor.webutils.queryparam.filterInternal.*;
import io.github.egorkor.webutils.queryparam.utils.FieldTypeUtils;
import jakarta.persistence.criteria.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.egorkor.webutils.queryparam.filterInternal.FilterOperation.*;

/**
 * Параметр запроса для фильтрации запрашиваемых ресурсов.
 *
 * <p>
 * Пример использования в контроллере:
 * <pre>{@code
 * @GetMapping
 * public List<User> controllerMethod(@ModelAttribute Filter<User> filter){
 *     return userDao.getAll(filter);
 * }
 * }</pre>
 * </p>
 *
 * <p>
 * <br>
 * Пример использования с JpaSpecificationExecutor, пагинация и сортировка
 * <pre>
 * {@code
 * public List<Entity> query(Filter<Entity> filter, SortParams sort, Pagination pagination){
 *     repository.findAll(filter,pagination.toJpaPageable(sort));
 * }}
 *
 * </pre>
 * </p>
 *
 * <p>
 * При интеграции с SQL следует использовать следующие методы:
 *     <ul>
 *         <li>{@link #toSQLFilter()}</li>
 *         <li>{@link #toSQLFilter(String prefix)}</li>
 *         <li>{@link #getFilterValues()}</li>
 *     </ul>
 *     <br>
 *     Пример использования Filter в связке с SQL на примере JdbcTemplate. Такое использование
 *     соответствует защищенному от SQL инъекций подходу с заменой параметров на знаки ?
 *     <pre>
 *         {@code
 *            public List<Entity> getAll(Filter<T> filter){
 *                //Generated WHERE clause -> WHERE name LIKE ? ESCAPE !
 *                String sql = String.format("SELECT * FROM users %s",filter.toSQLFilter())
 *                return jdbcTemplate.query(sql, USER_ROW_MAPPER, filter.getFilterValues());
 *            }
 *
 *         }
 *     </pre>
 * </p>
 * <p>
 * Для ограничения возможных инъекций параметров запросов, необходимо
 * выполнить наследования от данного класса, и определить там поля, которые
 * попадут в whiteList и будут допустимы к использованию как параметры запроса.
 * При этом если нужно задать специфичное имя, можно использовать аннотацию для
 * псевдонимов {@link FieldParamMapping}. Пример класса ограничивающего
 * возможный набор полей:
 *
 * <pre>
 * {@code
 * public class UserFilter{
 *     private String username;
 *     //Клиент присылает order_name, затем маппится в orders.name
 *     @FieldParamMapping(requestParamMapping="order_name", sqlMapping="orders.name")
 *     private String orderName;
 *     //Клиент присылает orders_count, затем маппится в orders.length()
 *     @FieldParamMapping(requestParamMapping="orders_count", sqlMapping="orders.length()")
 *     private Integer ordersCount;
 * }
 *
 * //Пример использования ограниченного набора полей при фильтрации
 * public class UserController{
 *     @GetMapping
 *     public List<User> getUsers(@ModelAttribute UserFilter filter){
 *         //...
 *     }
 * }
 * }
 * </pre>
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
//TODO: добавить поддержку операций работы с JSON
//TODO: добавить поддержку функций size() length() для SQL
//TODO: реализовать метод обновления по фильтру
@Slf4j
@Setter
@Getter
@ToString
public class Filter<T> implements Specification<T> {
    public final static FilterBuilder fb = new FilterBuilder();
    private static final Set<FilterOperation> NO_MAPPING_OPERATORS
            = Set.of(LS, LSE, EQUALS, GT, GTE);
    private static final Set<FilterOperation> BASIC_OPERATORS
            = Set.of(LS, LSE, EQUALS, GT, GTE, NOT_EQUALS);
    public static final Pattern FUNCTION_PATTERN = Pattern.compile("(.*)\\.(length\\(\\)|size\\(\\))");
    public static final Pattern CONCAT_FUNCTION_PATTERN = Pattern.compile("concat\\((.*)\\)");
    public static final Pattern TO_CHAR_FUNCTION_PATTERN = Pattern.compile("to_char\\((.*);'(.*)'\\)");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .toFormatter();
    protected List<FilterBasicOperation> operations;
    protected Class<?> entityType;
    protected List<Consumer<Root<T>>> queryConfigurers = new ArrayList<>();
    @JsonIgnore
    private List<String> fieldWhiteList = new ArrayList<>();
    @JsonIgnore
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
            default -> throw new IllegalArgumentException("Invalid comparison operation: " + operation);
        };
    }

    public static <X> Expression<X> getTypedExpression(Expression<?> expression, Class<X> type) {
        return (Expression<X>) expression;
    }

    public boolean isFiltered() {
        return !operations.isEmpty();
    }

    //region SQL Native Mapping

    public boolean isUnfiltered() {
        return operations.isEmpty();
    }

    public <R extends Filter<?>> R _and(Filter<?> filter) {
        this.operations.addAll(filter.getOperations());
        this.fieldWhiteList.addAll(
                filter.getOperations()
                        .stream()
                        .map(FilterBasicOperation::field)
                        .toList());
        return _this();
    }

    public String toSQLFilter() {
        return toSQLFilter("");
    }

    public String toSQLFilter(String prefix) {
        checkAllowedFilterFields();
        mapFilterByAllies();
        if (operations.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int filterIndex = 0; filterIndex < operations.size() - 1; filterIndex++) {
            if (sb.isEmpty()) {
                sb.append("WHERE ");
            }
            sb.append(parseCondition(operations.get(filterIndex), prefix));
            sb.append(" AND ");
        }
        if (sb.isEmpty() && !operations.isEmpty()) {
            sb.append("WHERE ");
            sb.append(parseCondition(operations.getLast(), prefix));
        }
        return sb.toString().trim();
    }

    private String parseCondition(FilterBasicOperation op, String prefix) {
        String field = op.field();
        FilterOperation operation = op.operation();
        Object value = op.value();

        return switch (operation) {
            case EQUALS, NOT_EQUALS, GT, GTE, LS, LSE -> buildBasicCondition(prefix + field, operation);
            case IS -> buildIsCondition(prefix + field, (Is) value);
            case CONTAINS, NOT_LIKE -> buildLikeCondition(prefix + field, false);
            case NOT_CONTAINS, LIKE -> buildLikeCondition(prefix + field, true);
            case NOT_IN -> buildInCondition(prefix + field, (List<Object>) value, true);
            case IN -> buildInCondition(prefix + field, (List<Object>) value, false);
        };
    }

    private String validateFieldName(String field) {
        if (!field.matches("[a-zA-Z0-9_.]+")) {
            throw new IllegalArgumentException("Invalid field name");
        }
        return field;
    }

    private String buildBasicCondition(String field, FilterOperation operator) {
        if (!BASIC_OPERATORS.contains(operator)) {
            throw new IllegalArgumentException("Invalid operator");
        }
        return String.format("%s %s ?", field, operator);
    }
    //endregion

    private String buildIsCondition(String field, Is value) {
        return switch (value) {
            case TRUE -> field + " = true";
            case FALSE -> field + " = false";
            case NULL -> field + " IS NULL";
            case NOT_NULL -> field + " IS NOT NULL";
        };
    }

    private String buildLikeCondition(String field, boolean not) {
        return "%s %sLIKE ? ESCAPE '!'".formatted(field, not ? "NOT " : "");
    }

    /**
     * Возвращает значения фильтров для подстановки в PreparedStatement.
     * Автоматически обрабатывает LIKE и IN условия.
     */
    public Object[] getFilterValues() {
        List<Object> values = new ArrayList<>();

        //TODO

        return values.toArray();
    }

    private String[] parseInValues(String value) {
        return Arrays.stream(value.split(";")).map(
                "'%s'"::formatted
        ).toArray(String[]::new);
    }

    private Object parseValue(String value, String operation) {
        try {
            if (operation.equals("is")) {
                return parseIsValue(value);
            }
            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value for operation " + operation + ": " + value, e);
        }
    }

    private Object parseIsValue(String value) {
        return switch (value.toLowerCase()) {
            case "true" -> true;
            case "false" -> false;
            case "null" -> null;
            case "not_null" -> "NOT NULL";
            default -> throw new IllegalArgumentException("Invalid IS value: " + value);
        };
    }

    private String buildInCondition(String field, List<Object> values, boolean not) {
        String placeholders = String.join(",", Collections.nCopies(values.size(), "?"));
        return String.format("%s %sIN (%s)", field, not ? "NOT " : "", placeholders);
    }

    private String mapOperation(FilterOperation operation) {
        if (NO_MAPPING_OPERATORS.contains(operation)) {
            return operation.getOperation();
        }
        return switch (operation) {
            case NOT_EQUALS -> "<>";
            case LIKE, CONTAINS -> "LIKE";
            case NOT_LIKE, NOT_CONTAINS -> "NOT LIKE";
            case IN -> "IN";
            case NOT_IN -> "NOT IN";
            case IS -> "IS";
            default -> operation.getOperation();
        };
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
        checkAllowedFilterFields();
        mapFilterByAllies();
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

    public <R extends Filter<?>> R withFetchJoin(String fetchingProperty) {
        this.fetchingProperties.add(fetchingProperty);
        queryConfigurers.add((root) -> {
            if (!fetchingProperty.contains(".")) {
                root.fetch(fetchingProperty, JoinType.LEFT);
            } else {
                String[] attributes = fetchingProperty.split("\\.");
                if (attributes.length > 2) {
                    throw new IllegalArgumentException("Invalid fetching property, allowed nested level is 2: " + fetchingProperty);
                }
                String parentAttribute = attributes[0];
                String secondAttribute = attributes[1];
                Fetch<?, ?> parentFetch = root.getFetches()
                        .stream()
                        .filter(f -> f.getAttribute().getName().equals(parentAttribute))
                        .toList().getFirst();
                parentFetch.fetch(secondAttribute, JoinType.LEFT);
            }

        });
        return _this();
    }

    public static <T> Filter<T> equals(Map<String, String> equalsFilters) {
        return new Filter<>(
                equalsFilters.entrySet()
                        .stream()
                        .map(filterUnit -> "%s:=:%s".formatted(filterUnit.getKey(), filterUnit.getValue()))
                        .toList()
        );
    }

    public static <T> Filter<T> equals(String field, Object value) {
        return fb.buildAnd(fb.equals(field, value));
    }

    public static <T> Filter<T> notEquals(String field, Object value) {
        return fb.buildAnd(fb.notEquals(field, value));
    }

    public static <T> Filter<T> contains(String field, String value) {
        return fb.buildAnd(fb.like(field, value));
    }

    public static <T> Filter<T> notContains(String field, String value) {
        return fb.buildAnd(fb.notLike(field, value));
    }

    public static <T> Filter<T> like(String field, String value) {
        return fb.buildAnd(fb.like(field, value));
    }

    public static <T> Filter<T> notLike(String field, String value) {
        return fb.buildAnd(fb.notLike(field, value));
    }

    public static <T> Filter<T> is(String field, Is value) {
        return fb.buildAnd(fb.is(field, value));
    }

    public static <T> Filter<T> in(String field, Object... values) {
        return fb.buildAnd(fb.in(field, values));
    }

    public static <T> Filter<T> notIn(String field, Object... values) {
        return fb.buildAnd(fb.notIn(field, values));
    }

    public static <T> Filter<T> greaterThan(String field, Comparable<?> value) {
        return fb.buildAnd(fb.greater(field, value));
    }

    public static <T> Filter<T> greaterThanOrEqual(String field, Comparable<?> value) {
        return fb.buildAnd(fb.greater(field, value));
    }

    public static <T> Filter<T> lessThan(String field, Comparable<?> value) {
        return fb.buildAnd(fb.less(field, value));
    }

    public static <T> Filter<T> lessThanOrEqual(String field, Comparable<?> value) {
        return fb.buildAnd(fb.lessOrEquals(field, value));
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
        Class<?> fieldType = reflectionField != null ? reflectionField.getType() : null;

        try {

            return switch (operation) {
                case IS -> parseIsPredicate(cb, selection, (Is) value);
                case EQUALS -> parseEqualPredicate(cb, selection, reflectionField, value, function);
                case GT, LS, GTE, LSE ->
                        parseComparisonPredicate(cb, selection, operation, reflectionField, value, function);
                case NOT_EQUALS -> parseNotEqualPredicate(cb, selection, fieldType, value, function);
                case CONTAINS -> parseContainsPredicate(cb, selection, value.toString());
                case NOT_CONTAINS -> cb.not(parseContainsPredicate(cb, selection, value.toString()));
                case LIKE -> parseLikePredicate(cb, selection, value.toString());
                case NOT_LIKE -> cb.not(parseLikePredicate(cb, selection, value.toString()));
                case IN -> parseInPredicate(cb, selection, reflectionField, (List<Object>) value, function);
                case NOT_IN -> cb.not(parseInPredicate(cb, selection, reflectionField, (List<Object>) value, function));
            };
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Error processing filter '%s' for field '%s': %s",
                            filter, field, e.getMessage()), e);
        }
    }

    private static <T> Expression<?> getSelectExpression(Root<T> root, String field, CriteriaBuilder cb) {
        if (!field.startsWith("concat")) {
            return field.contains(".") ? getNestedPath(root, field) : root.get(field);
        }
        Matcher concatMatcher = CONCAT_FUNCTION_PATTERN.matcher(field);
        if (!concatMatcher.matches()) {
            throw new IllegalArgumentException("Invalid concat function syntax: " + field);
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
                                       List<Object> inValues,
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
        Class<?> fieldType = reflectionField == null ? null : reflectionField.getType();
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
        if (value.getClass() != String.class) {
            throw new IllegalArgumentException("Cannot map value due to value type %s and target type %s"
                    .formatted(value.getClass().getSimpleName(), targetType.getSimpleName()));
        }
        String stringValue = value.toString();
        try {
            if (targetType == null) return value;
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

            throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Cannot convert '%s' to %s: %s",
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
            Object convertedValue = convertValue(value, getCollectionElementType(reflectionField));
            if (function != null) {
                return switch (function) {
                    case LENGTH, SIZE -> cb.equal(getFunctionPath(cb, selection, function), convertedValue);
                };
            }
            return cb.isMember(convertedValue, (Expression<Collection>) selection);
        }
        value = convertValue(value, reflectionField == null ? null : reflectionField.getType());
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
            throw new IllegalArgumentException("Selection attribute " + selection + " is not comparable");
        }

        Expression<Comparable> comparablePath = (Expression<Comparable>) getFunctionPath(cb, selection, function);

        if (reflectionField != null && Collection.class.isAssignableFrom(reflectionField.getType())) {
            Comparable<?> convertedValue = (Comparable<?>) convertValue(value, getCollectionElementType(reflectionField));
            if (function != null) {
                return switch (function) {
                    case LENGTH, SIZE -> getComparisonPredicate(cb, operation, comparablePath, convertedValue);
                };
            }
            return cb.isMember(convertedValue, (Path<Collection>) selection);
        }

        Class<?> type = reflectionField == null ? null : reflectionField.getType();
        Comparable<?> comparableValue = (Comparable<?>) convertValue(value, type);
        return getComparisonPredicate(cb, operation, comparablePath, comparableValue);
    }

    //endregion


    //region Utility Methods

    private Predicate parseNotEqualPredicate(CriteriaBuilder cb,
                                             Expression<?> selection,
                                             Class<?> fieldType,
                                             Object value,
                                             Function function) {
        value = convertValue(value, fieldType);
        return cb.notEqual(getFunctionPath(cb, selection, function), value);
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

    private void mapFilterByAllies() {
        if (this.getClass() == Filter.class) {
            return;
        }
        //TODO починить

        //ParamValidationUtils.mapParamsByFilter(operations, this.getClass());
    }

    private void checkAllowedFilterFields() {
        if (this.getClass() == Filter.class) {
            return;
        }
        //TODO починить

        /*ParamValidationUtils.validateAllowedParams(operations, this.getClass(),
                ParamValidationUtils.ParamType.FILTER, fieldWhiteList);*/
    }

    //endregion


}
