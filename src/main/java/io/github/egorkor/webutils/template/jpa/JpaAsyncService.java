package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.query.Filter;
import io.github.egorkor.webutils.query.PageableResult;
import io.github.egorkor.webutils.query.Pagination;
import io.github.egorkor.webutils.query.Sorting;
import io.github.egorkor.webutils.service.async.AsyncCRUDLService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor
public class JpaAsyncService<T, ID> implements AsyncCRUDLService<T, ID> {
    protected final JpaRepository<T, ID> jpaRepository;
    protected final JpaSpecificationExecutor<T> jpaSpecificationExecutor;
    protected final TransactionTemplate transactionTemplate;
    private Class<T> type;

    {
        Type superclass = getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) superclass;
        Type typeArgument = parameterizedType.getActualTypeArguments()[0];
        this.type = (Class<T>) typeArgument;
    }

    private String getEntityName() {
        return type == null ? "" : type.getSimpleName();
    }

    @Async
    @Override
    public CompletableFuture<PageableResult<List<T>>> getAllAsync(Filter<T> filter, Sorting sorting, Pagination pagination) {
        return CompletableFuture.supplyAsync(() ->
                transactionTemplate.execute(status ->
                        PageableResult.of(
                                jpaSpecificationExecutor.findAll(filter, pagination.toJpaPageable(sorting)).toList(),
                                jpaSpecificationExecutor.count(filter),
                                pagination.getSize()
                        ))
        );
    }

    @Async
    @Override
    public CompletableFuture<T> getByIdAsync(ID id) {
        return CompletableFuture.supplyAsync(() ->
                transactionTemplate.execute(status ->
                        jpaRepository.findById(id).orElseThrow(() ->
                                new ResourceNotFoundException("Сущность " + getEntityName()
                                        + " c id = " + id + " не найдена."))
                )
        );
    }

    @Async
    @Override
    public CompletableFuture<T> createAsync(T model) {
        return CompletableFuture.supplyAsync(
                () -> transactionTemplate.execute(status -> jpaRepository.save(model))
        );
    }

    @Async
    @Override
    public CompletableFuture<T> updateAsync(T model) {
        return CompletableFuture.supplyAsync(
                () -> transactionTemplate.execute(status -> jpaRepository.save(model))
        );
    }

    @Async
    @Override
    public CompletableFuture<Void> deleteAsync(ID id) {
        return CompletableFuture.runAsync(
                () -> transactionTemplate.executeWithoutResult(status -> jpaRepository.deleteById(id))
        );
    }
}
