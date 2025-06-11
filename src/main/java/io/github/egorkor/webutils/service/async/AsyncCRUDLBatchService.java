package io.github.egorkor.webutils.service.async;

import io.github.egorkor.webutils.service.batching.BatchResult;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AsyncCRUDLBatchService<T, ID> extends AsyncCRUDLService<T, ID> {
    /**
     * Асинхронное пакетное сохранение, выполняется не атомарно, при провале
     * одной операции, выполнение продолжается
     * */
    CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models);

    /**
     * Асинхронное пакетное обновление, выполняется не атомарно, при провале
     * одной операции, выполнение продолжается
     * */
    CompletableFuture<List<BatchResult>> batchUpdateAsync(List<T> models);

    /**
     * Асинхронное пакетное удаление, выполняется не атомарно, при провале
     * одной операции, выполнение продолжается
     * */
    CompletableFuture<List<BatchResult>> batchDeleteAsync(List<ID> ids);

    /** Асинхронное атомарное сохранение, при провале одной операции
     * выполнение прерывается, транзакция откатывается
     * */
    CompletableFuture<List<T>> batchCreateAtomic(List<T> models);

    /** Асинхронное атомарное обновление, при провале одной операции
     * выполнение прерывается, транзакция откатывается
     * */
    CompletableFuture<List<T>> batchUpdateAtomic(List<T> models);

    /** Асинхронное атомарное удаление, при провале одной операции
     * выполнение прерывается, транзакция откатывается
     * */
    CompletableFuture<Void> batchDeleteAtomic(List<ID> ids);
}
