package io.github.egorkor.query;

import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

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
@Data
public class Sorting {
    private List<String> sort = new ArrayList<>();

    public String toSQLSort() {
        return toSQLSort("");
    }

    public String toSQLSort(String prefix) {
        StringBuilder sb = new StringBuilder();
        for (String s : sort) {
            if (sb.isEmpty()) {
                sb.append("ORDER BY ");
            }
            String[] sortParts = s.split(",");
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

    public Sort toJpaSort() {
        if (sort.isEmpty()) {
            return Sort.unsorted();
        }
        List<Sort.Order> orders = sort.stream()
                .map(s -> s.split(","))
                .map(param -> new Sort.Order(
                        Sort.Direction.fromString(param[1].toLowerCase()),
                        param[0].toLowerCase()
                ))
                .toList();
        return Sort.by(orders);
    }

}
