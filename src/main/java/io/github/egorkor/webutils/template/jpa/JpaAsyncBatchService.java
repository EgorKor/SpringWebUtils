package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.service.async.AsyncCrudBatchService;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public abstract class JpaAsyncBatchService<T, ID> extends JpaAsyncCrudService<T, ID>
        implements AsyncCrudBatchService<T, ID> {

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final JpaBatchCrudService batchCrudService;

    public JpaAsyncBatchService(JpaRepository<T, ID> jpaRepository,
                                JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                                ApplicationEventPublisher eventPublisher,
                                TransactionTemplate transactionTemplate,
                                ThreadPoolTaskExecutor executor) {
        super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate, executor);
        Supplier<EntityManager> entityManagerSupplier = this::getPersistenceAnnotatedEntityManager;
        this.batchCrudService = new JpaBatchCrudService(jpaRepository,
                jpaSpecificationExecutor,
                eventPublisher,
                transactionTemplate) {
            @Override
            public EntityManager getPersistenceAnnotatedEntityManager() {
                return entityManagerSupplier.get();
            }
        };
    }


    @Async
    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(
                () -> batchCrudService.batchCreate(models, batchSize), executor);
    }


    @Async
    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchUpdateAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(() -> batchCrudService.batchUpdate(models, batchSize), executor);
    }

    @Async
    @Override
    public CompletableFuture<List<BatchResultWithData<ID>>> batchDeleteAsync(List<ID> ids, int batchSize) {
        return CompletableFuture.supplyAsync(() -> batchCrudService.batchDelete(ids, batchSize), executor);

    }

    @Override
    public CompletableFuture<List<T>> batchCreateAtomicAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(() -> batchCrudService.batchCreateAtomic(models, batchSize), executor);
    }

    @Override
    public CompletableFuture<List<T>> batchUpdateAtomicAsync(List<T> models, int batchSize) {
        return CompletableFuture.supplyAsync(() -> batchCrudService.batchUpdateAtomic(models, batchSize), executor);
    }

    @Override
    public CompletableFuture<Void> batchDeleteAtomicAsync(List<ID> ids, int batchSize) {
        return CompletableFuture.runAsync(() -> batchCrudService.batchDeleteAtomic(ids, batchSize), executor);
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
