package io.github.egorkor.webutils.template;

import io.github.egorkor.webutils.service.async.AsyncCRUDLBatchService;
import io.github.egorkor.webutils.service.batching.BatchOperationStatus;
import io.github.egorkor.webutils.service.batching.BatchResult;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class JpaAsyncBatchService<T, ID> extends JpaAsyncService<T, ID>
        implements AsyncCRUDLBatchService<T, ID> {

    private final TransactionTemplate transactionTemplate;
    private final JpaBatchService<T, ID> batchService;

    public JpaAsyncBatchService(JpaRepository<T, ID> jpaRepository,
                                JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                                TransactionTemplate transactionTemplate) {
        super(jpaRepository, jpaSpecificationExecutor);
        this.transactionTemplate = transactionTemplate;
        this.batchService = new JpaBatchService<>(jpaRepository, jpaSpecificationExecutor);
    }

    @Async
    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models) {
        return CompletableFuture.supplyAsync(
                () -> transactionTemplate.execute(status -> batchService.batchCreate(models)));
    }


    @Async
    @Override
    public CompletableFuture<List<BatchResult>> batchUpdateAsync(List<T> models) {
        return CompletableFuture.supplyAsync(() ->
                transactionTemplate.execute(status -> batchService.batchUpdate(models)));
    }

    @Async
    @Override
    public CompletableFuture<List<BatchResult>> batchDeleteAsync(List<ID> ids) {
        return CompletableFuture.supplyAsync(() ->
                transactionTemplate.execute(status -> batchService.batchDelete(ids)));

    }

    @Override
    public CompletableFuture<List<T>> batchCreateAtomic(List<T> models) {
        return CompletableFuture.supplyAsync(() ->
                transactionTemplate.execute(status -> batchService.batchCreateAtomic(models))
        );
    }

    @Override
    public CompletableFuture<List<T>> batchUpdateAtomic(List<T> models) {
        return CompletableFuture.supplyAsync(
                () -> transactionTemplate.execute(status -> batchService.batchUpdateAtomic(models))
        );
    }

    @Override
    public CompletableFuture<Void> batchDeleteAtomic(List<ID> ids) {
        return CompletableFuture.runAsync(
                () -> batchService.batchDeleteAtomic(ids)
        );
    }
}
