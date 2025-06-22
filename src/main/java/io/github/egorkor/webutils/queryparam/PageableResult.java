package io.github.egorkor.webutils.queryparam;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * PageableResult - класс обёртка для результата запроса с учётом пагинации.
 * Пример оборачивания результата запроса в PageableResult:
 * <pre>
 *     {@code
 *     public PageableResult<List<User>> getAll(Filter filter, Pagination pagination){
 *         return PageableResult.of(userRepository.findAll(pagination.toJpaPageable(),
 *            filter.toJpaFilter()), userRepository.count(filter.toJpaFilter()), pagination.getPageSize());
 *     }
 *     }
 * </pre>
 *
 * @author EgorKor
 * @since 2025
 */
@Getter
@AllArgsConstructor
@ToString
public class PageableResult<T> {
    private List<T> data;
    private long count;
    private long pageCount;
    private long pageSize;

    public static <T> PageableResult<T> of(Page<T> page) {
        return of(page.stream().toList(), page.getTotalElements(), page.getTotalPages(), page.getSize());
    }

    public static <T> PageableResult<T> of(List<T> data, long totalElements, long pageCount, long pageSize) {
        return new PageableResult<>(data, totalElements, pageCount, pageSize);
    }

    public static <T> PageableResult<T> of(List<T> data, long count, long pageSize) {
        return new PageableResult<>(data, count, countPages(count, pageSize), pageSize);
    }

    public static int countPages(long count, long pageSize) {
        return (int) Math.ceil((double) count / pageSize);
    }

    public <R> PageableResult<R> map(Function<? super List<T>, ? extends List<R>> mapper) {
        return new PageableResult<>(mapper.apply(data), count, pageCount, pageSize);
    }
}
