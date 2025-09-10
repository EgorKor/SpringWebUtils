package io.github.egorkor.webutils.queryparam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.egorkor.webutils.annotations.FieldParamMapping;
import io.github.egorkor.webutils.queryparam.utils.FieldTypeUtils;
import io.github.egorkor.webutils.queryparam.utils.ParamValidationUtils;
import jakarta.persistence.criteria.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

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
//TODO: добавить динамическое исключение физически выбираемых полей для hibernate
//TODO: реализовать метод обновления по фильтру
@Slf4j
@Setter
@Getter
@ToString
public class Filter<T> implements Specification<T> {
    public final static FilterBuilder fb = new FilterBuilder();
    private static final Set<String> NO_MAPPING_OPERATORS
            = Set.of("<", "<=", "=", ">=", ">");
    private static final Set<String> BASIC_OPERATORS
            = Set.of("<", "<=", "=", ">=", ">", "<>");
    private static final String FUNCTION_REGEX = "(length\\(\\))|(size\\(\\))";
    protected List<String> filter;
    protected Class<?> entityType;
    protected List<Consumer<Root<T>>> queryConfigurers = new ArrayList<>();
    @JsonIgnore
    private List<String> fieldWhiteList = new ArrayList<>();
    @JsonIgnore
    private List<String> fetchingProperties = new ArrayList<>();

    public Filter() {
        this.filter = new ArrayList<>();
        determineEntityType();
    }

    public Filter(List<String> filter) {
        this.filter = new ArrayList<>(filter);
        determineEntityType();
    }

    public Filter(Class<T> entityType) {
        this.entityType = entityType;
        this.filter = new ArrayList<>();
    }

    public Filter(List<String> filter, Class<?> entityType) {
        this.filter = filter;
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

    public static <T> Filter<T> softDeleteFilter(Field field, boolean isDeleted) {
        return softDeleteFilter(field.getName(), field.getType(), isDeleted);
    }


    public static <T> Filter<T> softDeleteFilter(Field field, boolean isDeleted, Class<T> entityType) {
        Filter<T> softDeleteFilter = softDeleteFilter(field.getName(), field.getType(), isDeleted);
        softDeleteFilter.setEntityType(entityType);
        return softDeleteFilter;
    }

    public static <T> Filter<T> softDeleteFilter(String fieldName, Class<?> fieldType, boolean isDeleted) {
        Filter<T> filter = new Filter<>();
        List<String> filterList = new ArrayList<>();
        if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
            filterList.add("%s:is:%s".formatted(fieldName, isDeleted));
        } else {
            filterList.add("%s:is:%s".formatted(fieldName, isDeleted ? "not_null" : "null"));
        }
        filter.setFilter(filterList);
        return filter;
    }

    public static <T> Filter<T> empty() {
        return new Filter<>();
    }

    public static <T> Filter<T> empty(Class<T> entityType) {
        return new Filter<>(entityType);
    }

    private static Predicate getComparisonPredicate(CriteriaBuilder cb,
                                                    String operation,
                                                    Expression<Comparable> comparablePath,
                                                    Comparable value) {
        return switch (operation) {
            case ">" -> cb.greaterThan(comparablePath, value);
            case "<" -> cb.lessThan(comparablePath, value);
            case ">=" -> cb.greaterThanOrEqualTo(comparablePath, value);
            case "<=" -> cb.lessThanOrEqualTo(comparablePath, value);
            default -> throw new IllegalArgumentException("Invalid comparison operation: " + operation);
        };
    }

    public static <X> Path<X> getTypedPath(Path<?> path, Class<X> type) {
        return (Path<X>) path;
    }

    public boolean isFiltered() {
        return !filter.isEmpty();
    }

    //region SQL Native Mapping

    public boolean isUnfiltered() {
        return filter.isEmpty();
    }

    public <R> Filter<R> concat(Filter<R> filter) {
        this.filter.addAll(filter.getFilter());
        this.fieldWhiteList.addAll(filter.getFilter()
                .stream().map(
                        s -> validateAndSplitFilter(s)[0]
                ).toList());
        return _this();
    }

    public String toSQLFilter() {
        return toSQLFilter("");
    }

    public String toSQLFilter(String prefix) {
        checkAllowedFilterFields();
        mapFilterByAllies();
        if (filter.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int filterIndex = 0; filterIndex < filter.size() - 1; filterIndex++) {
            if (sb.isEmpty()) {
                sb.append("WHERE ");
            }
            String[] orFilters = filter.get(filterIndex).split(":or:");
            for (int orFilterIndex = 0; orFilterIndex < orFilters.length; orFilterIndex++) {
                sb.append(parseCondition(orFilters[orFilterIndex], prefix));
                if (orFilterIndex < orFilters.length - 1) {
                    sb.append(" OR ");
                }
            }
            sb.append(" AND ");
        }
        if (sb.isEmpty() && !filter.isEmpty()) {
            sb.append("WHERE ");
        }
        String[] orFilters = filter.getLast().split(":or:");
        for (int orFilterIndex = 0; orFilterIndex < orFilters.length; orFilterIndex++) {
            sb.append(parseCondition(orFilters[orFilterIndex], prefix));
            if (orFilterIndex < orFilters.length - 1) {
                sb.append(" OR ");
            }
        }
        return sb.toString().trim();
    }

    private String parseCondition(String filter, String prefix) {
        String[] parts = validateAndSplitFilter(filter);
        String field = validateFieldName(parts[0]);
        String operation = mapOperation(parts[1].toLowerCase());
        String value = parts[2];

        return switch (operation) {
            case "=", "<>", ">", "<", ">=", "<=" -> buildBasicCondition(prefix + field, operation);
            case "IS" -> buildIsCondition(prefix + field, value);
            case "LIKE" -> buildLikeCondition(prefix + field, false);
            case "NOT LIKE" -> buildLikeCondition(prefix + field, true);
            case "NOT IN" -> buildInCondition(prefix + field, value, true);
            case "IN" -> buildInCondition(prefix + field, value, false);
            default -> throw new IllegalArgumentException("Invalid operation: " + operation);
        };
    }

    private String[] validateAndSplitFilter(String filter) {
        String[] parts = filter.split(":");
        if (parts.length == 3) {
            return parts;
        }
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid filter format");
        }
        String[] newParts = new String[3];
        newParts[0] = validateFieldName(parts[0]);
        newParts[1] = parts[1];
        newParts[2] = "";
        for (int i = 2; i < parts.length; i++) {
            newParts[2] += parts[i];
            if (i != parts.length - 1) {
                newParts[2] += ":";
            }
        }

        return newParts;
    }

    private String validateFieldName(String field) {
        if (!field.matches("[a-zA-Z0-9_.]+")) {
            throw new IllegalArgumentException("Invalid field name");
        }
        return field;
    }

    private String buildBasicCondition(String field, String operator) {
        if (!BASIC_OPERATORS.contains(operator)) {
            throw new IllegalArgumentException("Invalid operator");
        }
        return String.format("%s %s ?", field, operator);
    }
    //endregion

    private String buildIsCondition(String field, String value) {
        return switch (value) {
            case "true" -> field + " = true";
            case "false" -> field + " = false";
            case "null" -> field + " IS NULL";
            case "not_null" -> field + " IS NOT NULL";
            default -> throw new IllegalArgumentException("Invalid IS value");
        };
    }

    private String buildLikeCondition(String field, boolean not) {
        return "%s %sLIKE ? ESCAPE '!'".formatted(field, not ? "NOT " : "");
    }

    private String escapeLikeValue(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");
    }

    /**
     * Возвращает значения фильтров для подстановки в PreparedStatement.
     * Автоматически обрабатывает LIKE и IN условия.
     */
    public Object[] getFilterValues() {
        List<Object> values = new ArrayList<>();

        for (String filter : this.filter) {
            String[] parts = validateAndSplitFilter(filter);
            String operation = parts[1].toLowerCase();
            String value = parts[2];

            switch (operation) {
                case "like", "not_like":
                    values.add("%" + escapeLikeValue(value) + "%");
                    break;
                case "in", "not_in":
                    Collections.addAll(values, parseInValues(value));
                    break;
                case "is":
                    continue;
                default:
                    values.add(parseValue(value, operation));
            }
        }

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

    private String buildInCondition(String field, String value, boolean not) {
        String[] values = value.split(";");
        String placeholders = String.join(",", Collections.nCopies(values.length, "?"));
        return String.format("%s %sIN (%s)", field, not ? "NOT " : "", placeholders);
    }

    private String mapOperation(String operation) {
        if (NO_MAPPING_OPERATORS.contains(operation)) {
            return operation;
        }
        return switch (operation.toLowerCase()) {
            case "!=" -> "<>";
            case "like" -> "LIKE";
            case "not_like" -> "NOT LIKE";
            case "in" -> "IN";
            case "not_in" -> "NOT IN";
            case "is" -> "IS";
            default -> throw new IllegalArgumentException("Invalid filter operation: " + filter);
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
        filter.forEach(f -> {
            String[] filters = f.split(";or;");
            for (String filter : filters) {
                String field = validateAndSplitFilter(filter)[0];
                if (predicates.containsKey(field)) {
                    predicates.get(field).add(parsePredicate(filter, root, cb));
                } else {
                    predicates.put(field, new ArrayList<>(List.of(parsePredicate(filter, root, cb))));
                }
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

    public <R> Filter<R> configureQuery(Consumer<Root<T>> queryConfigurer) {
        queryConfigurers.add(queryConfigurer);
        return _this();
    }

    public <R> Filter<R> withFetchJoin(String fetchingProperty) {
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

    protected Predicate collectPredicates(CriteriaBuilder cb,
                                          Map<String, List<Predicate>> predicates) {
        return cb.and(predicates.values().stream()
                .flatMap(Collection::stream)
                .toList().toArray(new Predicate[0]));
    }

    private Predicate parsePredicate(String filter,
                                     Root<T> root,
                                     CriteriaBuilder cb) {
        String[] parts = validateAndSplitFilter(filter);

        String field = parts[0];
        String operation = parts[1].toLowerCase();
        String stringValue = parts[2];

        Function function = null;
        if (field.contains(".")) {
            String[] subFields = field.split("\\.");
            String lastSubField = subFields[subFields.length - 1];
            if (lastSubField.toLowerCase().matches(FUNCTION_REGEX)) {
                function = Function.parseByOperation(lastSubField);
                field = String.join(".", Arrays.copyOfRange(subFields, 0, subFields.length - 1));
            }
        }

        Path<?> path = field.contains(".") ? getNestedPath(root, field) : root.get(field);
        Field reflectionField = FieldTypeUtils.getField(entityType, field);
        Class<?> fieldType = reflectionField.getType();

        try {
            return switch (operation) {
                case "is" -> parseIsPredicate(cb, path, stringValue);
                case "=" -> parseEqualPredicate(cb, path, reflectionField, stringValue, function);
                case ">", "<", ">=", "<=" ->
                        parseComparisonPredicate(cb, path, operation, reflectionField, stringValue, function);
                case "!=" -> parseNotEqualPredicate(cb, path, fieldType, stringValue, function);
                case "like" -> parseLikePredicate(cb, path, stringValue);
                case "not_like" -> cb.not(parseLikePredicate(cb, path, stringValue));
                case "in" -> parseInPredicate(cb, path, reflectionField, stringValue, function);
                case "not_in" -> cb.not(parseInPredicate(cb, path, reflectionField, stringValue, function));
                default -> throw new IllegalArgumentException("Invalid filter operation: " + operation);
            };
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Error processing filter '%s' for field '%s' (type %s): %s",
                            filter, field, fieldType.getSimpleName(), e.getMessage()), e);
        }
    }

    private Predicate parseInPredicate(CriteriaBuilder cb,
                                       Path<?> path,
                                       Field reflectionField,
                                       String stringValue,
                                       Function function) {
        String[] stringValues = stringValue.split(";");

        //Если есть функция size или length

        if (Collection.class.isAssignableFrom(reflectionField.getType())) {
            Class<?> elementType = getCollectionElementType(reflectionField);
            if (function != null) {
                Object[] values = Arrays.stream(stringValues)
                        .map(v -> convertValue(v, elementType))
                        .toArray();
                return getFunctionPath(cb, path, function).in(values);
            }


            List<Predicate> predicates = new ArrayList<>();
            for (String strVal : stringValues) {
                Object val = convertValue(strVal, elementType);
                predicates.add(cb.isMember(val, (Path<Collection>) path));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        }
        // Для обычных полей
        Object[] values = Arrays.stream(stringValues)
                .map(v -> convertValue(v, reflectionField.getType()))
                .toArray();
        return path.in(values);
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

    private Object convertValue(String stringValue, Class<?> targetType) {
        if (stringValue == null) return null;

        try {
            if (targetType == String.class) return stringValue;
            if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(stringValue);
            if (targetType == Long.class || targetType == long.class) return Long.parseLong(stringValue);
            if (targetType == Double.class || targetType == double.class) return Double.parseDouble(stringValue);
            if (targetType == Float.class || targetType == float.class) return Float.parseFloat(stringValue);
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(stringValue);
            if (targetType.isEnum()) return Enum.valueOf((Class<Enum>) targetType, stringValue);

            throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Cannot convert '%s' to %s: %s",
                            stringValue, targetType.getSimpleName(), e.getMessage()), e);
        }
    }

    private Predicate parseIsPredicate(CriteriaBuilder cb, Path<?> path, String value) {
        return switch (value) {
            case "true" -> cb.isTrue(getTypedPath(path, Boolean.class));
            case "false" -> cb.isFalse(getTypedPath(path, Boolean.class));
            case "null" -> cb.isNull(path);
            case "not_null" -> cb.isNotNull(path);
            default -> throw new IllegalArgumentException("Invalid is-operation value: " + value);
        };
    }

    private Predicate parseEqualPredicate(CriteriaBuilder cb, Path<?> path, Field reflectionField, String stringValue, Function function) {
        if (Collection.class.isAssignableFrom(reflectionField.getType())) {
            Object convertedValue = convertValue(stringValue, getCollectionElementType(reflectionField));
            if (function != null) {
                return switch (function) {
                    case LENGTH, SIZE -> cb.equal(getFunctionPath(cb, path, function), convertedValue);
                };
            }
            return cb.isMember(convertedValue, (Path<Collection>) path);
        }
        Object value = convertValue(stringValue, reflectionField.getType());
        return cb.equal(getFunctionPath(cb, path, function), value);
    }

    private Expression<?> getFunctionPath(CriteriaBuilder cb, Path<?> current, Function function) {
        if (function == null) {
            return current;
        }
        return switch (function) {
            case LENGTH -> cb.length(getTypedPath(current, String.class));
            case SIZE -> cb.size(getTypedPath(current, Collection.class));
        };

    }

    private Predicate parseComparisonPredicate(CriteriaBuilder cb, Path<?> path, String operation,
                                               Field reflectionField, String stringValue, Function function) {
        if (!Comparable.class.isAssignableFrom(reflectionField.getType())
                && function == null) {
            throw new IllegalArgumentException("Field " + path + " is not comparable");
        }

        Expression<Comparable> comparablePath = (Expression<Comparable>) getFunctionPath(cb, path, function);

        if (Collection.class.isAssignableFrom(reflectionField.getType())) {
            Comparable<?> convertedValue = (Comparable<?>) convertValue(stringValue, getCollectionElementType(reflectionField));
            if (function != null) {
                return switch (function) {
                    case LENGTH, SIZE -> getComparisonPredicate(cb, operation, comparablePath, convertedValue);
                };
            }
            return cb.isMember(convertedValue, (Path<Collection>) path);
        }

        Comparable<?> value = (Comparable<?>) convertValue(stringValue, reflectionField.getType());
        return getComparisonPredicate(cb, operation, comparablePath, value);
    }

    //endregion


    //region Utility Methods

    private Predicate parseNotEqualPredicate(CriteriaBuilder cb,
                                             Path<?> path,
                                             Class<?> fieldType,
                                             String stringValue,
                                             Function function) {
        Object value = convertValue(stringValue, fieldType);
        return cb.notEqual(getFunctionPath(cb, path, function), value);
    }

    private Predicate parseLikePredicate(CriteriaBuilder cb, Path<?> path, String stringValue) {
        Path<String> stringPath = getTypedPath(path, String.class);
        return cb.like(stringPath, "%" + stringValue + "%");
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

    private <SameType> Filter<SameType> _this() {
        return (Filter<SameType>) this;
    }

    private void mapFilterByAllies() {
        if (this.getClass() == Filter.class) {
            return;
        }
        ParamValidationUtils.mapParamsByFilter(filter, this.getClass(),
                this::validateAndSplitFilter);
    }

    private void checkAllowedFilterFields() {
        if (this.getClass() == Filter.class) {
            return;
        }
        ParamValidationUtils.validateAllowedParams(filter, this.getClass(),
                ParamValidationUtils.ParamType.FILTER, this::validateAndSplitFilter, fieldWhiteList);
    }

    //endregion


    //region NestedTypes
    @Getter
    @AllArgsConstructor
    public enum Function {
        LENGTH("length()"),
        SIZE("size()");


        private final String function;

        public static Function parseByOperation(String operation) {
            for (Function func : values()) {
                if (operation.equals(func.function)) {
                    return func;
                }
            }
            throw new IllegalArgumentException("Illegal operation: " + operation);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum Is {
        TRUE("true"),
        FALSE("false"),
        NULL("null"),
        NOT_NULL("not_null");

        private final String value;
    }


    @Getter
    @AllArgsConstructor
    public enum FilterOperation {
        EQUALS("="),
        NOT_EQUALS("!="),
        GT(">"),
        GTE(">="),
        LS("<"),
        LSE("<="),
        LIKE("like"),
        IS("is"),
        IN("in"),
        NOT_LIKE("not_like"),
        NOT_IN("not_in");


        private final String operation;
    }

    public interface FilterUnit {
        String toStringFilter();
    }

    public interface BuildableOperation {
        <T> Filter<T> build();

        <T> Filter<T> build(Class<?> entityType);

        <R extends Filter> R buildDerived(Class<R> resultType);
    }

    public static class FilterBuilder {

        public FilterUnit equals(String field, String value) {
            return new BasicOperation(field, FilterOperation.EQUALS, value);
        }

        public FilterUnit notEquals(String field, String value) {
            return new BasicOperation(field, FilterOperation.NOT_EQUALS, value);
        }

        public FilterUnit less(String field, String value) {
            return new BasicOperation(field, FilterOperation.LS, value);
        }

        public FilterUnit lessOrEquals(String field, String value) {
            return new BasicOperation(field, FilterOperation.LSE, value);
        }

        public FilterUnit greater(String field, String value) {
            return new BasicOperation(field, FilterOperation.GT, value);
        }

        public FilterUnit greaterOrEquals(String field, String value) {
            return new BasicOperation(field, FilterOperation.GTE, value);
        }

        public FilterUnit like(String field, String value) {
            return new BasicOperation(field, FilterOperation.LIKE, value);
        }

        public FilterUnit in(String field, String... values) {
            return new BasicOperation(field, FilterOperation.IN, String.join(";", values));
        }

        public FilterUnit in(String field, Iterable<String> values) {
            return new BasicOperation(field, FilterOperation.IN, String.join(";", values));
        }

        public FilterUnit is(String field, Is value) {
            return new BasicOperation(field, FilterOperation.IS, value.getValue());
        }

        public FilterUnit notLike(String field, String value) {
            return new BasicOperation(field, FilterOperation.NOT_LIKE, value);
        }

        public FilterUnit notIn(String field, String... values) {
            return new BasicOperation(field, FilterOperation.NOT_IN, String.join(";", values));
        }

        public BuildableOperation or(FilterUnit... units) {
            return new OrOperation(Arrays.asList(units));
        }

        public BuildableOperation and(FilterUnit... units) {
            return new AndOperation(Arrays.asList(units));
        }

    }

    public abstract static class BuildableFilterOperation {
        public <T> Filter<T> build() {
            return build(null);
        }

        public <T> Filter<T> build(Class<?> entityType) {

            return new Filter<>(
                    new ArrayList<>(getFilters().stream()
                            .map(FilterUnit::toStringFilter).toList()), entityType
            );
        }

        @SneakyThrows
        public <R extends Filter> R buildDerived(Class<R> resultType) {

            R derivedFilter = resultType.getDeclaredConstructor().newInstance();
            derivedFilter.setFilter(
                    new ArrayList<>(getFilters().stream()
                            .map(FilterUnit::toStringFilter).toList()));
            return derivedFilter;
        }

        protected abstract List<FilterUnit> getFilters();
    }

    protected static class BasicOperation implements FilterUnit {
        private final String field;
        private final FilterOperation operation;
        private final String value;

        public BasicOperation(String field, FilterOperation operation, String value) {
            this.field = field;
            this.operation = operation;
            this.value = value;
        }

        public String toStringFilter() {
            return "%s:%s:%s".formatted(field, operation.getOperation(), value);
        }
    }

    protected static class OrOperation extends BuildableFilterOperation implements BuildableOperation, FilterUnit {
        private final List<FilterUnit> filterUnits;

        public OrOperation(List<FilterUnit> filterUnits) {
            this.filterUnits = filterUnits;
        }

        @Override
        public String toStringFilter() {
            return String.join(":or:", filterUnits.stream().map(FilterUnit::toStringFilter).toList());
        }

        @Override
        protected List<FilterUnit> getFilters() {
            return filterUnits;
        }
    }

    protected static class AndOperation extends BuildableFilterOperation implements BuildableOperation {
        private final List<FilterUnit> filterUnits;

        public AndOperation(List<FilterUnit> filterUnits) {
            this.filterUnits = filterUnits;
        }

        @Override
        protected List<FilterUnit> getFilters() {
            return filterUnits;
        }
    }


    //endregion

}
