package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.service.async.AsyncCRUDLBatchService;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public abstract class JpaAsyncBatchService<T, ID> extends JpaBatchCrudService<T, ID>
        implements AsyncCRUDLBatchService<T, ID> {

    private static final int DEFAULT_BATCH_SIZE = 100;

    public JpaAsyncBatchService(JpaRepository<T, ID> jpaRepository,
                                JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                                TransactionTemplate transactionTemplate,
                                ApplicationEventPublisher publisher) {
        super(jpaRepository, jpaSpecificationExecutor, publisher, transactionTemplate);
    }


    @Async
    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(
                () -> this.batchCreate(models, batchSize));
    }


    @Async
    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchUpdateAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(() -> this.batchUpdate(models, batchSize));
    }

    @Async
    @Override
    public CompletableFuture<List<BatchResultWithData<ID>>> batchDeleteAsync(List<ID> ids, int batchSize) {
        return CompletableFuture.supplyAsync(() -> this.batchDelete(ids, batchSize));

    }

    @Override
    public CompletableFuture<List<T>> batchCreateAtomicAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(() -> this.batchCreateAtomic(models, batchSize));
    }

    @Override
    public CompletableFuture<List<T>> batchUpdateAtomicAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(() -> this.batchUpdateAtomic(models, batchSize));
    }

    @Override
    public CompletableFuture<Void> batchDeleteAtomicAsync(List<ID> ids, int batchSize) {
        return CompletableFuture.runAsync(() -> this.batchDeleteAtomic(ids, batchSize));
    }

    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models) {
        return batchCreateAsync(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchUpdateAsync(List<T> models) {
        return batchUpdateAsync(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public CompletableFuture<List<BatchResultWithData<ID>>> batchDeleteAsync(List<ID> ids) {
        return batchDeleteAsync(ids, DEFAULT_BATCH_SIZE);
    }

    @Override
    public CompletableFuture<List<T>> batchCreateAtomicAsync(List<T> models) {
        return batchCreateAtomicAsync(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public CompletableFuture<List<T>> batchUpdateAtomicAsync(List<T> models) {
        return batchUpdateAtomicAsync(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public CompletableFuture<Void> batchDeleteAtomicAsync(List<ID> ids) {
        return batchDeleteAtomicAsync(ids, DEFAULT_BATCH_SIZE);
    }
}
