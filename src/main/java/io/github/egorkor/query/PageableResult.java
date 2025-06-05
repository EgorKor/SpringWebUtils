package io.github.egorkor.query;

import lombok.AllArgsConstructor;

import java.util.function.Function;

@AllArgsConstructor
public class PageableResult<T> {
    private T data;
    private int count;
    private int pageCount;
    private int pageSize;

    public static <T> PageableResult<T> of(T data, int count, int pageSize) {
        return new PageableResult<>(data, count, countPages(count, pageSize), pageSize);
    }

    public static int countPages(int count, int pageSize) {
        return (int) Math.ceil((double) count / pageSize);
    }

    public <R> PageableResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageableResult<>(mapper.apply(data), count, pageCount, pageSize);
    }
}
