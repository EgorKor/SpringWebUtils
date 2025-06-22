package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.annotations.SoftDeleteFlag;
import io.github.egorkor.webutils.event.crud.*;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.exception.SoftDeleteUnsupportedException;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.PageableResult;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import io.github.egorkor.webutils.service.sync.CrudService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

public abstract class JpaCrudService<T, ID> implements CrudService<T, ID>, InitializingBean {

    protected final JpaRepository<T, ID> jpaRepository;
    protected final JpaSpecificationExecutor<T> jpaSpecificationExecutor;
    protected final ApplicationEventPublisher eventPublisher;
    protected final TransactionTemplate transactionTemplate;
    protected final Class<T> entityType;
    @Setter
    protected EntityManager entityManager;

    private static final Set<Class<?>> SUPPORTED_SOFT_DELETE_TYPES = Set.of(
            Boolean.class, boolean.class,
            Timestamp.class, LocalDateTime.class, LocalDate.class, LocalTime.class,
            Instant.class, OffsetDateTime.class, OffsetTime.class, Date.class
    );

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
    private final Map<Class<?>, Supplier<Object>> restoreFlagMapping = Map.of(
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
    protected boolean isSoftDeleteSupported = false;
    protected Field softDeleteField;

    public JpaCrudService(JpaRepository<T, ID> jpaRepository,
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

    @SneakyThrows
    @Override
    public void afterPropertiesSet() {
        this.entityManager = getPersistenceAnnotatedEntityManager();
    }

    public abstract EntityManager getPersistenceAnnotatedEntityManager();



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
            this.softDeleteField = softDeleteFields.getFirst();
        }
    }

    private String getEntityName() {
        return entityType == null ? "" : entityType.getSimpleName();
    }

    @Override
    public PageableResult<T> getAll(Filter<T> filter, Sorting sorting, Pagination pagination) {
        return PageableResult.of(jpaSpecificationExecutor.findAll(filter, pagination.toJpaPageable(sorting)));
    }

    @Override
    public T getById(ID id) throws ResourceNotFoundException {
        Supplier<ResourceNotFoundException> exceptionSupplier = () ->
                new ResourceNotFoundException("Сущность "
                        + getEntityName()
                        + " c id = "
                        + id
                        + " не найдена.");
        boolean isDeleted = false;
        return isSoftDeleteSupported ?
                jpaRepository.findById(id)
                        .orElseThrow(exceptionSupplier) :
                jpaSpecificationExecutor.findOne(Filter.softDeleteFilter(softDeleteField, isDeleted))
                        .orElseThrow(exceptionSupplier);
    }

    @Override
    public T getByFilter(Filter<T> filter) throws ResourceNotFoundException{
        Supplier<ResourceNotFoundException> exceptionSupplier = () ->
                new ResourceNotFoundException("Сущность "
                        + getEntityName()
                        + " по условию "
                        + filter.toSQLFilter().replace("WHERE", "").trim()
                        + " не найдена.");

        boolean isDeleted = false;
        return isSoftDeleteSupported ?
                jpaSpecificationExecutor.findOne(filter)
                        .orElseThrow(exceptionSupplier) :
                jpaSpecificationExecutor.findOne(
                                Filter.softDeleteFilter(softDeleteField, isDeleted)
                                        .andConcat(filter))
                        .orElseThrow(exceptionSupplier);
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
    public void deleteById(ID id) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new EntityDeletingEvent<>(this, id));
        }
        transactionTemplate.executeWithoutResult(status -> jpaRepository.deleteById(id));
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new EntityDeletedEvent<>(this, id, entityType));
        }
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public void deleteAll(Filter<T> filter) {
        jpaSpecificationExecutor.delete(filter);
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
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean exists(Filter<T> filter) {
        return jpaSpecificationExecutor.exists(filter);
    }

    private void checkSoftDeleteAvailability() {
        if (!isSoftDeleteSupported) {
            throw new SoftDeleteUnsupportedException("Soft operation delete is not supported");
        }
    }

    @SneakyThrows
    @Override
    public void softDeleteById(ID id) throws ResourceNotFoundException, SoftDeleteUnsupportedException {
        checkSoftDeleteAvailability();
        T entity = getById(id);
        softDeleteField.set(entity, softDeleteFlagMapping.get(softDeleteField.getType()).get());
        transactionTemplate.execute(status -> jpaRepository.save(entity));
    }

    @Override
    public void softDeleteAll() throws SoftDeleteUnsupportedException {
        softDelete(Filter.emptyFilter());
    }

    @Override
    public void softDelete(Filter<T> filter) throws SoftDeleteUnsupportedException {
        checkSoftDeleteAvailability();
        Object fieldValueObject = softDeleteFlagMapping.get(softDeleteField.getType()).get();
        String entityName = entityManager.getMetamodel().entity(entityType).getName();
        String hqlRestore = "UPDATE %s e SET e.%s = :deleteValue %s".formatted(
                entityName,
                softDeleteField.getName(),
                filter.toSQLFilter());
        Query query = entityManager.createQuery(hqlRestore)
                .setParameter("deleteValue", mapToHqlQueryParameter(fieldValueObject));
        transactionTemplate.executeWithoutResult(status -> query.executeUpdate());
    }

    protected String mapToHqlQueryParameter(Object param) {
        if (param == null) {
            return "null";
        }

        return switch (param) {
            case Instant instant -> String.format("'%s'", instant);
            case Timestamp timestamp ->
                    String.format("'%s'", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(timestamp));
            case LocalDateTime localDateTime ->
                    String.format("'%s'", localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            case LocalDate localDate -> String.format("'%s'", localDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            case LocalTime localTime -> String.format("'%s'", localTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
            case Date date -> String.format("'%s'", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(date));
            case Boolean bool -> bool.toString();
            case Number num -> num.toString();
            case String str -> String.format("'%s'", str.replace("'", "''"));
            default -> throw new IllegalArgumentException(
                    String.format("Unsupported parameter type %s for HQL mapping",
                            param.getClass().getSimpleName())
            );
        };
    }

    @SneakyThrows
    @Override
    public void restoreById(ID id) throws ResourceNotFoundException, SoftDeleteUnsupportedException {
        checkSoftDeleteAvailability();
        T entity = getById(id);
        softDeleteField.set(entity, restoreFlagMapping.get(softDeleteField.getType()).get());
        jpaRepository.save(entity);
    }

    @Override
    public void restoreAll() throws SoftDeleteUnsupportedException {
        restoreAll(Filter.emptyFilter());
    }

    @Override
    public void restoreAll(Filter<T> filter) throws SoftDeleteUnsupportedException {
        checkSoftDeleteAvailability();
        Object fieldValueObject = restoreFlagMapping.get(softDeleteField.getType()).get();
        String entityName = entityManager.getMetamodel().entity(entityType).getName();
        String hqlRestore = "UPDATE %s e SET e.%s = :restoreValue %s".formatted(
                entityName,
                softDeleteField.getName(),
                filter.toSQLFilter());
        Query query = entityManager.createQuery(hqlRestore)
                .setParameter("restoreValue", mapToHqlQueryParameter(fieldValueObject));
        transactionTemplate.executeWithoutResult(status -> query.executeUpdate());
    }


}
