package io.github.egorkor.webutils.service.async;

import io.github.egorkor.webutils.service.batching.BatchResult;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AsyncCRUDLBatchService<T, ID> extends AsyncCRUDLService<T, ID> {
    /**
     * Асинхронное пакетное сохранение, выполняется не атомарно, при провале
     * одной операции, выполнение продолжается
     */
    CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models);

    /**
     * Асинхронное пакетное обновление, выполняется не атомарно, при провале
     * одной операции, выполнение продолжается
     */
    CompletableFuture<List<BatchResultWithData<T>>> batchUpdateAsync(List<T> models);

    /**
     * Асинхронное пакетное удаление, выполняется не атомарно, при провале
     * одной операции, выполнение продолжается
     */
    CompletableFuture<List<BatchResultWithData<ID>>> batchDeleteAsync(List<ID> ids);

    /**
     * Асинхронное атомарное сохранение, при провале одной операции
     * выполнение прерывается, транзакция откатывается
     */
    CompletableFuture<List<T>> batchCreateAtomicAsync(List<T> models);

    /**
     * Асинхронное атомарное обновление, при провале одной операции
     * выполнение прерывается, транзакция откатывается
     */
    CompletableFuture<List<T>> batchUpdateAtomicAsync(List<T> models);

    /**
     * Асинхронное атомарное удаление, при провале одной операции
     * выполнение прерывается, транзакция откатывается
     */
    CompletableFuture<Void> batchDeleteAtomicAsync(List<ID> ids);

    /**
     * Асинхронное пакетное сохранение, выполняется не атомарно, при провале
     * одной операции, выполнение продолжается
     */
    CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models, int batchSize);

    /**
     * Асинхронное пакетное обновление, выполняется не атомарно, при провале
     * одной операции, выполнение продолжается
     */
    CompletableFuture<List<BatchResultWithData<T>>> batchUpdateAsync(List<T> models, int batchSize);

    /**
     * Асинхронное пакетное удаление, выполняется не атомарно, при провале
     * одной операции, выполнение продолжается
     */
    CompletableFuture<List<BatchResultWithData<ID>>> batchDeleteAsync(List<ID> ids, int batchSize);

    /**
     * Асинхронное атомарное сохранение, при провале одной операции
     * выполнение прерывается, транзакция откатывается
     */
    CompletableFuture<List<T>> batchCreateAtomicAsync(List<T> models, int batchSize);

    /**
     * Асинхронное атомарное обновление, при провале одной операции
     * выполнение прерывается, транзакция откатывается
     */
    CompletableFuture<List<T>> batchUpdateAtomicAsync(List<T> models, int batchSize);

    /**
     * Асинхронное атомарное удаление, при провале одной операции
     * выполнение прерывается, транзакция откатывается
     */
    CompletableFuture<Void> batchDeleteAtomicAsync(List<ID> ids, int batchSize);
}
