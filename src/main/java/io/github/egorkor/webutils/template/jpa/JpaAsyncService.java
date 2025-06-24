package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.PageableResult;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import io.github.egorkor.webutils.service.async.AsyncCRUDLService;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;


public abstract class JpaAsyncService<T, ID> extends JpaCrudService<T, ID> implements AsyncCRUDLService<T, ID> {
    protected EntityManager entityManager;
    protected Class<T> type;

    public JpaAsyncService(JpaRepository<T, ID> jpaRepository, JpaSpecificationExecutor<T> jpaSpecificationExecutor, ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate) {
        super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate);
    }


    private String getEntityName() {
        return type == null ? "" : type.getSimpleName();
    }

    @Async
    @Override
    public CompletableFuture<PageableResult<T>> getAllAsync(Filter<T> filter, Sorting sorting, Pagination pagination) {
        return CompletableFuture.supplyAsync(() -> this.getAll(filter, sorting, pagination));
    }

    @Async
    @Override
    public CompletableFuture<T> getByIdAsync(ID id) {
        return CompletableFuture.supplyAsync(() -> this.getById(id));
    }

    @Async
    @Override
    public CompletableFuture<T> createAsync(T model) {
        return CompletableFuture.supplyAsync(() -> this.create(model));
    }

    @Async
    @Override
    public CompletableFuture<T> updateAsync(T model) {
        return CompletableFuture.supplyAsync(() -> this.fullUpdate(model));
    }

    @Async
    @Override
    public CompletableFuture<Void> deleteAsync(ID id) {
        return CompletableFuture.runAsync(() -> this.deleteById(id));
    }
}
