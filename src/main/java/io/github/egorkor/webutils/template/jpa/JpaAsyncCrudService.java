package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.PageableResult;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import io.github.egorkor.webutils.service.async.AsyncCrudService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;


/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public abstract class JpaAsyncCrudService<T, ID> extends JpaCrudService<T, ID> implements AsyncCrudService<T, ID> {
    protected final ThreadPoolTaskExecutor executor;

    public JpaAsyncCrudService(JpaRepository<T, ID> jpaRepository,
                               JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                               ApplicationEventPublisher eventPublisher,
                               TransactionTemplate transactionTemplate,
                               ThreadPoolTaskExecutor executor
    ) {
        super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate);
        this.executor = executor;
    }

    @Async
    @Override
    public CompletableFuture<PageableResult<T>> getAllAsync(Filter<T> filter, Sorting sorting, Pagination pagination) {
        return CompletableFuture.supplyAsync(() -> this.getAll(filter, sorting, pagination), executor);
    }

    @Async
    @Override
    public CompletableFuture<T> getByIdAsync(ID id) {
        return CompletableFuture.supplyAsync(() -> this.getById(id), executor);
    }

    @Async
    @Override
    public CompletableFuture<T> createAsync(T model) {
        return CompletableFuture.supplyAsync(() -> this.create(model), executor);
    }

    @Async
    @Override
    public CompletableFuture<T> fullUpdateAsync(T model) {
        return CompletableFuture.supplyAsync(() -> this.fullUpdate(model), executor);
    }

    @Async
    @Override
    public CompletableFuture<Void> deleteByIdAsync(ID id) {
        return CompletableFuture.runAsync(() -> this.deleteById(id), executor);
    }

    @Async
    @Override
    public CompletableFuture<Long> countByFilterAsync(Filter<T> filter) {
        return CompletableFuture.supplyAsync(() -> this.countByFilter(filter), executor);
    }

    @Async
    @Override
    public CompletableFuture<Long> countAllAsync() {
        return CompletableFuture.supplyAsync(this::countAll, executor);
    }

    @Async
    @Override
    public CompletableFuture<Void> restoreByFilterAsync(Filter<T> filter) {
        return CompletableFuture.runAsync(() -> this.restoreByFilterAsync(filter), executor);
    }

    @Async
    @Override
    public CompletableFuture<Void> restoreAllAsync() {
        return CompletableFuture.runAsync(this::restoreAll, executor);
    }

    @Async
    @Override
    public CompletableFuture<Void> restoreByIdAsync(ID id) {
        return CompletableFuture.runAsync(() -> this.restoreById(id), executor);
    }

    @Async
    @Override
    public CompletableFuture<Void> softDeleteByFilterAsync(Filter<T> filter) {
        return CompletableFuture.runAsync(() -> this.softDeleteByFilter(filter), executor);
    }

    @Async
    @Override
    public CompletableFuture<Void> softDeleteAllAsync() {
        return CompletableFuture.runAsync(this::softDeleteAll, executor);
    }

    @Async
    @Override
    public CompletableFuture<Void> softDeleteByIdAsync(ID id) {
        return CompletableFuture.runAsync(() -> this.softDeleteById(id), executor);
    }

    @Async
    @Override
    public CompletableFuture<Void> deleteByFilterAsync(Filter<T> filter) {
        return CompletableFuture.runAsync(() -> this.deleteByFilter(filter), executor);
    }

    @Async
    @Override
    public CompletableFuture<Void> deleteAllAsync() {
        return CompletableFuture.runAsync(this::deleteAll, executor);
    }

    @Async
    @Override
    public CompletableFuture<T> patchUpdateAsync(ID id, T model) {
        return CompletableFuture.supplyAsync(() -> this.patchUpdate(id, model));
    }

    @Async
    @Override
    public CompletableFuture<T> getByFilterAsync(Filter<T> filter) {
        return CompletableFuture.supplyAsync(() -> this.getByFilter(filter), executor);
    }
}
