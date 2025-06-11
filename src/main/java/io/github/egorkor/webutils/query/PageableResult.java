package io.github.egorkor.webutils.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
public class PageableResult<T> {
    private T data;
    private long count;
    private long pageCount;
    private long pageSize;

    public static <T> PageableResult<T> of(T data, long count, long pageSize) {
        return new PageableResult<>(data, count, countPages(count, pageSize), pageSize);
    }

    public static int countPages(long count, long pageSize) {
        return (int) Math.ceil((double) count / pageSize);
    }

    public <R> PageableResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageableResult<>(mapper.apply(data), count, pageCount, pageSize);
    }
}
