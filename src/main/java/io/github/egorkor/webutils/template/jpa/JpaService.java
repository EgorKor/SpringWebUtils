package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.event.crud.*;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.query.Filter;
import io.github.egorkor.webutils.query.PageableResult;
import io.github.egorkor.webutils.query.Pagination;
import io.github.egorkor.webutils.query.Sorting;
import io.github.egorkor.webutils.service.sync.CRUDLService;
import lombok.SneakyThrows;
import org.hibernate.annotations.SoftDelete;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class JpaService<T, ID> implements CRUDLService<T, ID> {
    protected final JpaRepository<T, ID> jpaRepository;
    protected final JpaSpecificationExecutor<T> jpaSpecificationExecutor;
    protected final ApplicationEventPublisher eventPublisher;
    protected final TransactionTemplate transactionTemplate;
    protected final Class<T> entityType;

    protected boolean isSoftDeleteSupported = false;
    protected Field softDeleteField;


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
        defineSoftDeleteSupport();
    }

    private static final Set<Class<?>> SUPPORTED_SOFT_DELETE_TYPES = Set.of(
            Boolean.class, boolean.class,
            Timestamp.class, LocalDateTime.class, LocalDate.class, LocalTime.class,
            Instant.class, OffsetDateTime.class, OffsetTime.class, Date.class
    );

    private void defineSoftDeleteSupport() {
        if (this.entityType == null) {
            return;
        }
        List<Field> softDeleteFields = Arrays.stream(this.entityType.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(SoftDeleteFlag.class))
                .peek(field -> {
                    if (!SUPPORTED_SOFT_DELETE_TYPES.contains(field.getType())) {
                        throw new IllegalStateException(String.format(
                                "%s - field '%s' has unsupported type %s for soft-delete flag",
                                entityType.getName(),
                                field.getName(),
                                field.getType().getSimpleName()
                        ));
                    }
                })
                .toList();

        if (softDeleteFields.size() > 1) {
            throw new IllegalStateException(String.format(
                    "%s - only one soft-delete flag is supported, found %d",
                    entityType.getName(),
                    softDeleteFields.size()
            ));
        }

        if (!softDeleteFields.isEmpty()) {
            this.isSoftDeleteSupported = true;
            this.softDeleteField = softDeleteFields.get(0);
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
    public T fullUpdate(T model) {
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
    public T patchUpdate(ID id, T model) {
        T dbModel = getById(id);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new EntityUpdatingEvent<>(this, dbModel));
        }
        JpaEntityPropertyPatcher.patch(model, dbModel);
        T updated = transactionTemplate.execute(status -> jpaRepository.save(dbModel));
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

    @Override
    public long count(Filter<T> filter) {
        return jpaSpecificationExecutor.count(filter);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public boolean exists(ID id) {
        return false;
    }

    @Override
    public boolean exists(Filter<T> filter) {
        return jpaSpecificationExecutor.exists(filter);
    }

    private final Map<Class<?>, Supplier<Object>> softDeleteFlagMapping
            = new HashMap<>(Map.of(
            boolean.class, () -> true,
            Boolean.class, () -> Boolean.TRUE,
            Timestamp.class, () -> Timestamp.from(Instant.now()),
            LocalDateTime.class, LocalDateTime::now,
            LocalDate.class, LocalDateTime::now,
            LocalTime.class, LocalDateTime::now,
            Instant.class, Instant::now,
            OffsetDateTime.class, OffsetDateTime::now,
            OffsetTime.class, OffsetTime::now,
            Date.class, () -> new Date(System.currentTimeMillis())
    ));


    private final Map<Class<?>, Supplier<Object>> recoverFlagMapping = Map.of(
            boolean.class, () -> false,
            Boolean.class, () -> Boolean.FALSE,
            Timestamp.class, () -> null,
            LocalDateTime.class, () -> null,
            LocalDate.class, () -> null,
            LocalTime.class, () -> null,
            Instant.class, () -> null,
            OffsetDateTime.class, () -> null,
            OffsetTime.class, () -> null,
            Date.class, () -> null
    );

    private void checkSoftDeleteAvailability() {
        if (!isSoftDeleteSupported) {
            throw new IllegalStateException("Soft operation delete is not supported");
        }
    }

    @SneakyThrows
    @Override
    public void softDelete(ID id) {
        checkSoftDeleteAvailability();
        T entity = getById(id);
        softDeleteField.set(entity, softDeleteFlagMapping.get(softDeleteField.getType()).get());
        jpaRepository.save(entity);
    }

    @SneakyThrows
    @Override
    public void restore(ID id) {
        checkSoftDeleteAvailability();
        T entity = getById(id);
        softDeleteField.set(entity, recoverFlagMapping.get(softDeleteField.getType()).get());
        jpaRepository.save(entity);
    }
}
