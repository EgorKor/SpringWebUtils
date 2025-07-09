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
import java.util.stream.Collectors;

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
//TODO: Добавить поддержку операции .length() у поля
@Slf4j
@Setter
@Getter
@ToString
public class Filter<T> implements Specification<T> {
    private static final Set<String> NO_MAPPING_OPERATORS
            = Set.of("<", "<=", "=", ">=", ">");
    private static final Set<String> BASIC_OPERATORS
            = Set.of("<", "<=", "=", ">=", ">", "<>");
    private static final String FUNCTION_REGEX = "(length\\(\\))|(size\\(\\))";

    @JsonIgnore
    private List<String> fieldWhiteList = new ArrayList<>();
    protected List<String> filter;
    protected Class<?> entityType;
    protected List<Consumer<Root<T>>> queryConfigurers = new ArrayList<>();

    public Filter() {
        this.filter = new ArrayList<>();
        determineEntityType();
    }

    public Filter(List<String> filter) {
        this.filter = filter;
        determineEntityType();
    }

    public Filter(Class<T> entityType) {
        this.entityType = entityType;
        this.filter = new ArrayList<>();
    }

    public Filter(List<String> filter, Class<T> entityType) {
        this.filter = filter;
        this.entityType = entityType;
    }

    public boolean isFiltered() {
        return !filter.isEmpty();
    }

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
    //region SQL Native Mapping

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
        for (int i = 0; i < filter.size() - 1; i++) {
            if (sb.isEmpty()) {
                sb.append("WHERE ");
            }
            sb.append(parseCondition(filter.get(i), prefix, RequestType.SQL));
            sb.append(" AND ");
        }
        if (sb.isEmpty() && !filter.isEmpty()) {
            sb.append("WHERE ");
        }
        sb.append(parseCondition(filter.getLast(), prefix, RequestType.SQL));
        return sb.toString().trim();
    }

    public enum RequestType{
        SQL, HQL
    }

    private String parseCondition(String filter, String prefix, RequestType type) {
        String[] parts = validateAndSplitFilter(filter);
        String field = validateFieldName(parts[0]);
        String operation = mapOperation(parts[1].toLowerCase());
        String value = parts[2];

        return switch (operation) {
            case "=", "<>", ">", "<", ">=", "<=" -> buildBasicCondition(prefix + field, operation);
            case "IS" -> buildIsCondition(prefix + field, value);
            case "LIKE" -> buildLikeCondition(prefix + field);
            case "IN" -> buildInCondition(prefix + field, value);
            default -> throw new IllegalArgumentException("Invalid operation: " + operation);
        };
    }


    private String[] validateAndSplitFilter(String filter) {
        String[] parts = filter.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid filter format");
        }
        return parts;
    }

    private String validateFieldName(String field) {
        if (!field.matches("[a-zA-Z0-9_]+")) {
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

    private String buildIsCondition(String field, String value) {
        return switch (value) {
            case "true" -> field + " = true";
            case "false" -> field + " = false";
            case "null" -> field + " IS NULL";
            case "not_null" -> field + " IS NOT NULL";
            default -> throw new IllegalArgumentException("Invalid IS value");
        };
    }

    private String buildLikeCondition(String field) {
        return "%s LIKE ? ESCAPE '!'".formatted(field);
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
                case "like":
                    values.add("%" + escapeLikeValue(value) + "%");
                    break;
                case "in":
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

    private String buildInCondition(String field, String value) {
        String[] values = value.split(";");
        String placeholders = String.join(",", Collections.nCopies(values.length, "?"));
        return String.format("%s IN (%s)", field, placeholders);
    }

    private String mapOperation(String operation) {
        if (NO_MAPPING_OPERATORS.contains(operation)) {
            return operation;
        }
        return switch (operation.toLowerCase()) {
            case "!=" -> "<>";
            case "like" -> "LIKE";
            case "in" -> "IN";
            case "is" -> "IS";
            default -> throw new IllegalArgumentException("Invalid filter operation: " + filter);
        };
    }
    //endregion

    //region Criteria API Mapping
    @Override
    public Predicate toPredicate(Root<T> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        return toPredicate(root, cb);
    }

    public Predicate toPredicate(Root<T> root,
                                 CriteriaBuilder cb){
        checkAllowedFilterFields();
        mapFilterByAllies();
        if(queryConfigurers.isEmpty()) {
            configureQuery(root);
        }else{
            queryConfigurers.forEach(c -> c.accept(root));
        }
        Map<String, List<Predicate>> predicates = new HashMap<>();
        filter.forEach(f -> {
            String field = validateAndSplitFilter(f)[0];
            if (predicates.containsKey(field)) {
                predicates.get(field).add(parsePredicate(f, root, cb));
            } else {
                predicates.put(field, new ArrayList<>(List.of(parsePredicate(f, root, cb))));
            }
        });
        return collectPredicates(cb, predicates);
    }

    /**
     * Предназначен для переопределения,
     * например чтобы
     */
    protected void configureQuery(Root<T> root) {}

    public <R> Filter<R> configureQuery(Consumer<Root<T>> queryConfigurer) {
        queryConfigurers.add(queryConfigurer);
        return _this();
    }

    public <R> Filter<R> withFetchJoin(String fetchingProperty){
        queryConfigurers.add((root) -> {
            root.fetch(fetchingProperty, JoinType.LEFT);
        });
        return _this();
    }

    protected Predicate collectPredicates(CriteriaBuilder cb, Map<String, List<Predicate>> predicates) {
        return cb.and(predicates.values().stream()
                .flatMap(Collection::stream)
                .toList().toArray(new Predicate[0]));
    }

    private Predicate parsePredicate(String filter, Root<T> root, CriteriaBuilder cb) {
        String[] parts = filter.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid filter format. Expected: field:operation:value");
        }

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
                case "in" -> parseInPredicate(cb, path, reflectionField, stringValue);
                default -> throw new IllegalArgumentException("Invalid filter operation: " + operation);
            };
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Error processing filter '%s' for field '%s' (type %s): %s",
                            filter, field, fieldType.getSimpleName(), e.getMessage()), e);
        }
    }

    private Predicate parseInPredicate(CriteriaBuilder cb, Path<?> path, Field reflectionField, String stringValue) {
        String[] stringValues = stringValue.split(";");

        if (Collection.class.isAssignableFrom(reflectionField.getType())) {
            Class<?> elementType = getCollectionElementType(reflectionField);

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

    private Class<?> getCollectionElementType(Field field) {
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
                    default -> throw new IllegalArgumentException("Invalid function for equals operation: " + function);
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
            default -> throw new IllegalArgumentException("");
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
            Object convertedValue = convertValue(stringValue, getCollectionElementType(reflectionField));
            if (function != null) {
                return switch (function) {
                    case LENGTH, SIZE -> switch (operation) {
                        case ">" -> cb.greaterThan(comparablePath, (Comparable) convertedValue);
                        case "<" -> cb.lessThan(comparablePath, (Comparable) convertedValue);
                        case ">=" -> cb.greaterThanOrEqualTo(comparablePath, (Comparable) convertedValue);
                        case "<=" -> cb.lessThanOrEqualTo(comparablePath, (Comparable) convertedValue);
                        default -> throw new IllegalArgumentException("Invalid comparison operation: " + operation);
                    };
                    default -> throw new IllegalArgumentException("Invalid function for equals operation: " + function);
                };
            }
            return cb.isMember(convertedValue, (Path<Collection>) path);
        }

        Comparable<?> value = (Comparable<?>) convertValue(stringValue, reflectionField.getType());
        return switch (operation) {
            case ">" -> cb.greaterThan(comparablePath, (Comparable) value);
            case "<" -> cb.lessThan(comparablePath, (Comparable) value);
            case ">=" -> cb.greaterThanOrEqualTo(comparablePath, (Comparable) value);
            case "<=" -> cb.lessThanOrEqualTo(comparablePath, (Comparable) value);
            default -> throw new IllegalArgumentException("Invalid comparison operation: " + operation);
        };
    }

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

    private <X> Path<X> getTypedPath(Path<?> path, Class<X> type) {
        return (Path<X>) path;
    }

    public static <T> Path<T> getNestedPath(Root<T> root, String field) {
        String[] fields = field.split("\\.");
        Path<T> path = root.get(fields[0]);
        for (int i = 1; i < fields.length; i++) {
            path = path.get(fields[i]);
        }
        return path;
    }

    //endregion

    //region Universal Private Methods

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



    //region Utility Methods

    public static FilterBuilder builder() {
        return new FilterBuilder();
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

    public static <T> Filter<T> emptyFilter() {
        return new Filter<>();
    }

    public static <T> Filter<T> emptyFilter(Class<T> entityType) {
        return new Filter<>(entityType);
    }

    //endregion

    //region NestedTypes
    public static class FilterBuilder {
        protected final List<FilterUnit> filters = new ArrayList<>();

        public FilterBuilder equals(String field, String value) {
            filters.add(new FilterUnit(field, FilterOperation.EQUALS, value));
            return this;
        }

        public FilterBuilder notEquals(String field, String value) {
            filters.add(new FilterUnit(field, FilterOperation.NOT_EQUALS, value));
            return this;
        }

        public FilterBuilder less(String field, String value) {
            filters.add(new FilterUnit(field, FilterOperation.LS, value));
            return this;
        }

        public FilterBuilder lessOrEquals(String field, String value) {
            filters.add(new FilterUnit(field, FilterOperation.LSE, value));
            return this;
        }

        public FilterBuilder greater(String field, String value) {
            filters.add(new FilterUnit(field, FilterOperation.GT, value));
            return this;
        }

        public FilterBuilder greaterOrEquals(String field, String value) {
            filters.add(new FilterUnit(field, FilterOperation.GTE, value));
            return this;
        }

        public FilterBuilder like(String field, String value) {
            filters.add(new FilterUnit(field, FilterOperation.LIKE, value));
            return this;
        }

        public FilterBuilder in(String field, String... values) {
            filters.add(new FilterUnit(field, FilterOperation.IN, String.join(";", values)));
            return this;
        }

        public FilterBuilder is(String field, Is value) {
            filters.add(new FilterUnit(field, FilterOperation.IS, value.getValue()));
            return this;
        }

        public <T> Filter<T> build() {
            return new Filter<>(
                    new ArrayList<>(filters.stream().map(
                            (o) -> "%s:%s:%s".formatted(o.field(), o.filterOperation().getOperation(), o.value())
                    ).toList())
            );
        }

        @SneakyThrows
        public <R extends Filter> R buildDerived(Class<R> resultType) {
            R derivedFilter = resultType.getDeclaredConstructor().newInstance();
            derivedFilter.setFilter(
                    new ArrayList<>(filters.stream().map(
                            (o) -> "%s:%s:%s".formatted(o.field(), o.filterOperation().getOperation(), o.value())
                    ).toList()
                    ));
            return derivedFilter;
        }

    }

    public record FilterUnit(String field, FilterOperation filterOperation, String value) {
    }


    @Getter
    @AllArgsConstructor
    public enum Function {
        LENGTH("length()"),
        SIZE("size()"),
        SUM("sum()"),
        MAX("max()"),
        AVG("avg()"),
        MIN("min()");

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
        IN("in");


        private final String operation;
    }
    //endregion

}
