package io.github.egorkor.webutils.template;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AsyncCRUDLBatchService<T, ID> extends AsyncCRUDLService<T, ID> {
    CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models);

    CompletableFuture<List<BatchResult>> batchUpdateAsync(List<T> models);

    CompletableFuture<List<BatchResult>> batchDeleteAsync(List<ID> ids);
}
