package io.github.egorkor.webutils.service.async;

import io.github.egorkor.webutils.service.batching.BatchResult;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AsyncCRUDLBatchService<T, ID> extends AsyncCRUDLService<T, ID> {
    CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models);

    CompletableFuture<List<BatchResult>> batchUpdateAsync(List<T> models);

    CompletableFuture<List<BatchResult>> batchDeleteAsync(List<ID> ids);

    CompletableFuture<List<T>> batchCreateAtomic(List<T> models);

    CompletableFuture<List<T>> batchUpdateAtomic(List<T> models);

    CompletableFuture<Void> batchDeleteAtomic(List<ID> ids);
}
