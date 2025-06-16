package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.service.async.AsyncCRUDLBatchService;
import io.github.egorkor.webutils.service.batching.BatchResult;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public class JpaAsyncBatchService<T, ID> extends JpaAsyncService<T, ID>
        implements AsyncCRUDLBatchService<T, ID> {

    private final JpaBatchService<T, ID> batchService;
    private static final int DEFAULT_BATCH_SIZE = 100;

    public JpaAsyncBatchService(JpaRepository<T, ID> jpaRepository,
                                JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                                TransactionTemplate transactionTemplate,
                                EntityManager entityManager) {
        super(jpaRepository, jpaSpecificationExecutor, transactionTemplate);
        this.batchService = new JpaBatchService<>(jpaRepository, jpaSpecificationExecutor, transactionTemplate, entityManager);
    }

    @Async
    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(
                () ->batchService.batchCreate(models, batchSize));
    }


    @Async
    @Override
    public CompletableFuture<List<BatchResult>> batchUpdateAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(() -> batchService.batchUpdate(models, batchSize));
    }

    @Async
    @Override
    public CompletableFuture<List<BatchResult>> batchDeleteAsync(List<ID> ids, int batchSize) {
        return CompletableFuture.supplyAsync(() -> batchService.batchDelete(ids, batchSize));

    }

    @Override
    public CompletableFuture<List<T>> batchCreateAtomicAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(() -> batchService.batchCreateAtomic(models, batchSize));
    }

    @Override
    public CompletableFuture<List<T>> batchUpdateAtomicAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(() -> batchService.batchUpdateAtomic(models, batchSize));
    }

    @Override
    public CompletableFuture<Void> batchDeleteAtomicAsync(List<ID> ids, int batchSize) {
        return CompletableFuture.runAsync(() ->  batchService.batchDeleteAtomic(ids, batchSize));
    }

    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models) {
        return batchCreateAsync(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public CompletableFuture<List<BatchResult>> batchUpdateAsync(List<T> models) {
        return batchUpdateAsync(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public CompletableFuture<List<BatchResult>> batchDeleteAsync(List<ID> ids) {
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
