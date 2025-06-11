package io.github.egorkor.webutils.query;

import jakarta.persistence.criteria.*;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Driver;
import java.util.*;

/**
 * Параметр запроса для фильтрации запрашиваемых ресурсов.
 * Пример использования в контроллере:
 * <pre>{@code
 * public void controllerMethod(@RequestParam Filter filter)
 * }</pre>
 * Пример использования с JPA репозиториями сортировка,пагинация и сортировка
 * <pre>
 * {@code
 * public List<Entity> query(Filter filter, SortParams sort, Pagination pagination){
 *     repository.findAll(filter,pagination.toJpaPageable(sort));
 * }
 * }
 * </pre>
 *
 * @author EgorKor
 * @since 2025
 */
@Data
public class Filter<T> implements Specification<T> {
    private List<String> filter = new ArrayList<>();

    private Driver driver = null;

    private static final Set<String> NO_MAPPING_OPERATORS
            = Set.of("<", "<=", "=", ">=", ">");
    private static final Set<String> BASIC_OPERATORS
            = Set.of("<", "<=", "=", ">=", ">", "<>");

    public Filter<T> withSoftDeleteFlag() {
        filter.add("isDeleted:is:false");
        return this;
    }

    public Filter<T> withSoftDeleteTime() {
        filter.add("deletedAt:is:not_null");
        return this;
    }

    public Filter<T> withSoftDeleteFlag(String flagName) {
        filter.add("%s:is:false".formatted(flagName));
        return this;
    }

    public Filter<T> withSoftDeleteTime(String fieldName) {
        filter.add("%s:is:not_null".formatted(fieldName));
        return this;
    }

    public String toSQLFilter() {
        return toSQLFilter("");
    }

    public String toSQLFilter(String prefix) {
        if (filter.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filter.size() - 1; i++) {
            if (sb.isEmpty()) {
                sb.append(" WHERE ");
            }
            sb.append(parseCondition(filter.get(i), prefix));
            sb.append(" AND ");
        }
        if (sb.isEmpty() && !filter.isEmpty()) {
            sb.append(" WHERE ");
        }
        sb.append(parseCondition(filter.get(filter.size() - 1), prefix));
        return sb.toString();
    }


    private String parseCondition(String filter, String prefix) {
        String[] parts = filter.split(":");
        String field = parts[0];
        String operation = parts[1].toLowerCase();
        String value = parts[2];

        operation = mapOperation(operation);
        if (BASIC_OPERATORS.contains(operation)) {
            return "%s %s '%s'".formatted(prefix + field, operation, value);
        }
        return switch (operation) {
            case "IS" -> switch (value) {
                case "true" -> "%s = true".formatted(prefix + field);
                case "false" -> "%s = false".formatted(prefix + field);
                case "null" -> "%s IS NULL".formatted(prefix + field);
                case "not_null" -> "%s IS NOT NULL".formatted(prefix + field);
                default ->
                        throw new IllegalArgumentException("Invalid filter value: " + operation + "; for operation: " + value);
            };
            case "LIKE" -> "%s LIKE ".formatted(prefix + field) + "%" + value + "%";
            case "IN" -> {
                String inValues;
                if (value.contains(";")) {
                    inValues = "( " + String.join(" , ", Arrays
                            .stream(value.split(";"))
                            .map(s -> "'" + s + "'")
                            .toList()) + " )";
                } else {
                    inValues = "(" + value + ")";
                }
                yield "%s IN %s".formatted(prefix + field, inValues);
            }
            default -> throw new IllegalArgumentException("Invalid filter operation: " + operation);
        };

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
