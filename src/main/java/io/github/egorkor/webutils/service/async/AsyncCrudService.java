package io.github.egorkor.webutils.service.async;

import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.PageableResult;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;

import java.util.concurrent.CompletableFuture;


/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public interface AsyncCrudService<T, ID> {
    CompletableFuture<PageableResult<T>> getAllAsync(Filter<T> filter, Sorting sorting, Pagination pagination);

    CompletableFuture<T> getByIdAsync(ID id);

    CompletableFuture<T> getByFilterAsync(Filter<T> filter);

    CompletableFuture<T> createAsync(T model);

    CompletableFuture<T> fullUpdateAsync(T model);
    CompletableFuture<T> patchUpdateAsync(ID id, T model);

    CompletableFuture<Void> deleteByIdAsync(ID id);
    CompletableFuture<Void> deleteAllAsync();
    CompletableFuture<Void> deleteByFilterAsync(Filter<T> filter);

    CompletableFuture<Void> softDeleteByIdAsync(ID id);
    CompletableFuture<Void> softDeleteAllAsync();
    CompletableFuture<Void> softDeleteByFilterAsync(Filter<T> filter);

    CompletableFuture<Void> restoreByIdAsync(ID id);
    CompletableFuture<Void> restoreAllAsync();
    CompletableFuture<Void> restoreByFilterAsync(Filter<T> filter);

    CompletableFuture<Long> countAllAsync();
    CompletableFuture<Long> countByFilterAsync(Filter<T> filter);



}
