package io.github.egorkor.webutils.service.async;

import io.github.egorkor.webutils.query.Filter;
import io.github.egorkor.webutils.query.PageableResult;
import io.github.egorkor.webutils.query.Pagination;
import io.github.egorkor.webutils.query.Sorting;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AsyncCRUDLService<T, ID> {
    CompletableFuture<PageableResult<List<T>>> getAllAsync(Filter<T> filter, Sorting sorting, Pagination pagination);

    CompletableFuture<T> getByIdAsync(ID id);

    CompletableFuture<T> createAsync(T model);

    CompletableFuture<T> updateAsync(T model);

    CompletableFuture<Void> deleteAsync(ID id);
}
