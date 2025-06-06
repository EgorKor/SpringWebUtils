package io.github.egorkor.query;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Параметр запроса для пагинации запрашиваемых ресурсов.
 * Пример использования в контроллере:
 * <pre>{@code
 * public void controllerMethod(@RequestParam Pagination pagination)
 * }</pre>
 * Пример использования с JPA репозиториями сортировка и пагинация
 * <pre>
 * {@code
 * public List<Entity> query(SortParams sort, Pagination pagination){
 *     repository.findAll(pagination.toJpaPageable(sort));
 * }
 * }
 * </pre>
 *
 * @author EgorKor
 * @since 2025
 */
@Data
public class Pagination {
    private int size;
    private int page = 10;

    public Pageable toJpaPageable() {
        return PageRequest.of(page, size);
    }

    public Pageable toJpaPageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }

    public Pageable toJpaPageable(Sorting sorting) {
        return PageRequest.of(page, size, sorting.toJpaSort());
    }

    public String toSQLPageable() {
        return "OFFSET %d LIMIT %d".formatted(page * size, size);
    }
}
