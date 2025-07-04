package io.github.egorkor.webutils.queryparam;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static io.github.egorkor.webutils.queryparam.Filter.getNestedPath;

/**
 * Параметр запроса для сортировки запрашиваемых ресурсов.
 * Пример использования в контроллере:
 * <pre>{@code
 * public void controllerMethod(@RequestParam SortParams sort)
 * }</pre>
 * Пример использования с JPA репозиториями
 * <pre>
 * {@code
 * public List<Entity> query(SortParams sort){
 *     repository.findAll(sort.toJpaSort());
 * }
 * }
 * </pre>
 *
 * @author EgorKor
 * @since 2025
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Sorting {
    private List<String> sort = new ArrayList<>();

    public static Sorting unsorted() {
        return new Sorting();
    }

    public boolean isUnsorted() {
        return sort.isEmpty();
    }

    public String toSQLSort() {
        return toSQLSort("");
    }

    public String toSQLSort(String prefix) {
        StringBuilder sb = new StringBuilder();
        for (String s : sort) {
            if (sb.isEmpty()) {
                sb.append("ORDER BY ");
            }
            String[] sortParts = s.split(":");
            sb.append(prefix)
                    .append(sortParts[0])
                    .append(" ")
                    .append(sortParts[1].toUpperCase())
                    .append(", ");
        }
        if (!sb.isEmpty()) {
            sb.deleteCharAt(sb.length() - 2);
        }
        return sb.toString();
    }

    public <T> List<Order> toCriteriaOrderList(Root<T> root, CriteriaBuilder cb) {
        List<Order> orderList = new ArrayList<>();
        for (String s : sort) {
            String[] sortParts = s.split(":");
            String field = sortParts[0];
            String order = sortParts[1];
            Path<T> path = field.contains(".") ? getNestedPath(root, field) : root.get(field);
            if (order.equalsIgnoreCase("asc")) {
                orderList.add(cb.asc(path));
            } else if (order.equalsIgnoreCase("desc")) {
                orderList.add(cb.desc(path));
            }
        }
        return orderList;
    }


    public Sort toJpaSort() {
        if (sort.isEmpty()) {
            return Sort.unsorted();
        }
        List<Sort.Order> orders = sort.stream()
                .map(s -> s.split(":"))
                .map(param -> new Sort.Order(
                        Sort.Direction.fromString(param[1].toLowerCase()),
                        param[0].toLowerCase()
                ))
                .toList();
        return Sort.by(orders);
    }

    public static SortingBuilder builder() {
        return new SortingBuilder();
    }

    public static class SortingBuilder {
        private final List<SortingUnit> sorting = new ArrayList<>();

        public SortingBuilder asc(String field) {
            sorting.add(new SortingUnit(field, "asc"));
            return this;
        }

        public SortingBuilder desc(String field) {
            sorting.add(new SortingUnit(field, "desc"));
            return this;
        }

        public Sorting build() {
            return new Sorting(new ArrayList<>(sorting.stream()
                    .map(s -> "%s:%s".formatted(s.field(), s.order())).toList()));
        }

    }

    public record SortingUnit(String field, String order) { }

}
