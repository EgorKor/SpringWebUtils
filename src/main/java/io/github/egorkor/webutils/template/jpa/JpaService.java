package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.event.crud.*;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.query.Filter;
import io.github.egorkor.webutils.query.PageableResult;
import io.github.egorkor.webutils.query.Pagination;
import io.github.egorkor.webutils.query.Sorting;
import io.github.egorkor.webutils.service.sync.CRUDLService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class JpaService<T, ID> implements CRUDLService<T, ID> {
    protected final JpaRepository<T, ID> jpaRepository;
    protected final JpaSpecificationExecutor<T> jpaSpecificationExecutor;
    protected final ApplicationEventPublisher eventPublisher;
    protected final TransactionTemplate transactionTemplate;
    protected final Class<T> entityType;


    public JpaService(JpaRepository<T, ID> jpaRepository,
                      JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                      ApplicationEventPublisher eventPublisher,
                      TransactionTemplate transactionTemplate) {
        this.jpaRepository = jpaRepository;
        this.jpaSpecificationExecutor = jpaSpecificationExecutor;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;

        //initialize entity class definition
        {
            Type superclass = getClass().getGenericSuperclass();
            ParameterizedType parameterizedType = (ParameterizedType) superclass;
            Type typeArgument = parameterizedType.getActualTypeArguments()[0];
            this.entityType = (Class<T>) typeArgument;
        }
    }

    private String getEntityName() {
        return entityType == null ? "" : entityType.getSimpleName();
    }

    @Override
    public PageableResult<List<T>> getAll(Filter<T> filter, Sorting sorting, Pagination pagination) {
        return PageableResult.of(
                jpaSpecificationExecutor.findAll(filter, pagination.toJpaPageable(sorting)).toList(),
                jpaSpecificationExecutor.count(filter),
                pagination.getSize()
        );
    }

    @Override
    public T getById(ID id) {
        return jpaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Сущность "
                                + getEntityName()
                                + " c id = "
                                + id
                                + " не найдена."));
    }

    @Override
    public T create(T model) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new EntityCreatingEvent<>(this, model));
        }
        T saved = transactionTemplate.execute(status -> jpaRepository.save(model));
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new EntityCreatedEvent<>(this, saved));
        }
        return saved;
    }

    @Override
    public T update(T model) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new EntityUpdatingEvent<>(this, model));
        }
        T updated = transactionTemplate.execute(status -> jpaRepository.save(model));
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new EntityUpdatedEvent<>(this, updated));
        }
        return updated;
    }

    @Override
    public void delete(ID id) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new EntityDeletingEvent<>(this, id));
        }
        transactionTemplate.executeWithoutResult(status -> {
            jpaRepository.deleteById(id);
        });
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new EntityDeletedEvent<>(this, id, entityType));
        }
    }
}
