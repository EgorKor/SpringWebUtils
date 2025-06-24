package io.github.egorkor.webutils.queryparam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.egorkor.webutils.annotations.FieldAllies;
import jakarta.persistence.criteria.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Параметр запроса для фильтрации запрашиваемых ресурсов.
 *
 * <p>
 * Пример использования в контроллере:
 * <pre>{@code
 * @GetMapping
 * public List<User> controllerMethod(@RequestParam Filter<User> filter){
 *     return userDao.getAll(filter);
 * }
 * }</pre>
 * </p>
 *
 * <p>
 * При интеграции с JPA после передаче в JpaSpecificationExecutor фильтра, будет
 * вызван следующий метод
 * <ul>
 *     <li>{@link #toPredicate(Root, CriteriaQuery, CriteriaBuilder)}</li>
 * </ul>
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
 *     При интеграции с SQL следует использовать следующие методы:
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
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@NoArgsConstructor
@Data
public class Filter<T> implements Specification<T> {
    private static final Set<String> NO_MAPPING_OPERATORS
            = Set.of("<", "<=", "=", ">=", ">");
    private static final Set<String> BASIC_OPERATORS
            = Set.of("<", "<=", "=", ">=", ">", "<>");

    @JsonIgnore
    private List<String> fieldWhiteList = new ArrayList<>();
    private List<String> filter = new ArrayList<>();

    public Filter(List<String> filter) {
        this.filter = filter;
    }

    public static <T> Filter<T> softDeleteFilter(Field field, boolean isDeleted) {
        return softDeleteFilter(field.getName(), field.getType(), isDeleted);
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

    private <SameType> Filter<SameType> _this() {
        return (Filter<SameType>) this;
    }

    private void mapFilterByAllies() {
        if (this.getClass() == Filter.class) {
            return;
        }
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            FieldAllies fieldAllies = field.getAnnotation(FieldAllies.class);
            if (fieldAllies == null) {
                continue;
            }
            String alliesName = fieldAllies.value();
            String fieldName = field.getName();
            String regexSafeFieldName = Pattern.quote(fieldName);
            for (int i = 0; i < filter.size(); i++) {
                String filterFieldName = validateAndSplitFilter(filter.get(i))[0];
                if (fieldName.equals(filterFieldName)) {
                    filter.set(i, filter.get(i)
                            .replaceFirst(regexSafeFieldName, alliesName));
                    break;
                }
            }
        }
    }

    private void checkAllowedFilterFields() {
        if (this.getClass() == Filter.class) {
            return;
        }
        Set<String> filterFieldsNames = filter.stream().map(
                s -> validateAndSplitFilter(s)[0]
        ).collect(Collectors.toSet());

        Set<String> allowedFields = Arrays.stream(this.getClass().getDeclaredFields())
                .map(f -> {
                    FieldAllies allies;
                    if ((allies = f.getAnnotation(FieldAllies.class)) != null) {
                        return allies.value();
                    } else {
                        return f.getName();
                    }
                })
                .collect(Collectors.toSet());

        filterFieldsNames.removeAll(allowedFields);
        fieldWhiteList.forEach(filterFieldsNames::remove);
        if (!filterFieldsNames.isEmpty()) {
            throw new IllegalArgumentException("Illegal parameters in filter: " + filterFieldsNames);
        }
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
        for (int i = 0; i < filter.size() - 1; i++) {
            if (sb.isEmpty()) {
                sb.append("WHERE ");
            }
            sb.append(parseCondition(filter.get(i), prefix));
            sb.append(" AND ");
        }
        if (sb.isEmpty() && !filter.isEmpty()) {
            sb.append("WHERE ");
        }
        sb.append(parseCondition(filter.getLast(), prefix));
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

    @Override
    public Predicate toPredicate(Root<T> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        checkAllowedFilterFields();
        mapFilterByAllies();
        List<Predicate> predicates = new ArrayList<>();
        filter.forEach(f ->
                predicates.add(parsePredicate(f, root, query, cb))
        );
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private Predicate parsePredicate(String filter,
                                     Root<T> root,
                                     CriteriaQuery<?> query,
                                     CriteriaBuilder cb) {
        String[] parts = filter.split(":");

        String field = parts[0];
        String operation = parts[1].toLowerCase();
        String value = parts[2];


        Path<T> path = field.contains(".") ? getNestedPath(root, field) : root.get(field);
        return switch (operation) {
            case "is" -> switch (value) {
                case "true" -> cb.isTrue((Path<Boolean>) path);
                case "false" -> cb.isFalse((Path<Boolean>) path);
                case "null" -> cb.isNull(path);
                case "not_null" -> cb.isNotNull(path);
                default ->
                        throw new IllegalArgumentException("Invalid filter value: " + operation + "; for operation: " + value);
            };
            case "=" -> cb.equal(path, value);
            case ">" -> cb.greaterThan(path.as(Comparable.class), (Comparable) value);
            case "<" -> cb.lessThan(path.as(Comparable.class), (Comparable) value);
            case ">=" -> cb.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
            case "<=" -> cb.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
            case "!=" -> cb.notEqual(path.as(Comparable.class), value);
            case "like" -> cb.like(path.as(String.class), "%" + value + "%");
            case "in" -> path.in((Object[]) value.split(";"));
            default -> throw new IllegalArgumentException("Invalid filter operation: " + operation);
        };
    }

    private Path<T> getNestedPath(Root<T> root, String field) {
        String[] fields = field.split("\\.");
        Path<T> path = root.get(fields[0]);
        for (int i = 1; i < fields.length; i++) {
            path = path.get(fields[i]);
        }
        return path;
    }


}
