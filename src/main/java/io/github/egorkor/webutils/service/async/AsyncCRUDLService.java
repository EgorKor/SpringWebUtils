package io.github.egorkor.webutils.service.async;

import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.PageableResult;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;

import java.util.concurrent.CompletableFuture;

public interface AsyncCRUDLService<T, ID> {
    CompletableFuture<PageableResult<T>> getAllAsync(Filter<T> filter, Sorting sorting, Pagination pagination);

    CompletableFuture<T> getByIdAsync(ID id);

    CompletableFuture<T> createAsync(T model);

    CompletableFuture<T> updateAsync(T model);

    CompletableFuture<Void> deleteAsync(ID id);
}
