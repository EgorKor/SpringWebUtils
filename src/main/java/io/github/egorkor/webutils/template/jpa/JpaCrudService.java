package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.annotations.SoftDeleteFlag;
import io.github.egorkor.webutils.event.crud.*;
import io.github.egorkor.webutils.exception.EntityOperation;
import io.github.egorkor.webutils.exception.EntityProcessingException;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.exception.SoftDeleteUnsupportedException;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.PageableResult;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import io.github.egorkor.webutils.service.sync.CrudService;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;


/**
 * Данный класс реализует интерфейс {@link CrudService}
 * используя стандарт JPA и Hibernate ORM в виде JPA провайдера.
 * Данная реализация предоставляет встроенное
 * определение поддержки мягкого удаления сущности посредством
 * использования аннотации @SoftDeleteFlag в классе сущности T,
 * оставленной над полем следующих типов данных:
 * <ul>
 *     <li>{@link Boolean}</li>
 *     <li>{@link Timestamp}</li>
 *     <li>{@link Instant}</li>
 *     <li>{@link LocalDateTime}</li>
 *     <li>{@link OffsetDateTime}</li>
 *     <li>{@link Date}</li>
 *
 * </ul>
 * <p>
 * При выполнении операций происходит генерация событий
 * на которые можно подписаться стандартным для Spring способом, используя
 * аннотацию {@link org.springframework.context.event.EventListener}
 * <br>
 * Список генерируемых событий:
 * <table border="1">
 *     <tr>
 *         <th>Метод</th>
 *         <th>События</th>
 *     </tr>
 *     <tr>
 *         <td>{@link #create(Object)}</td>
 *         <td>{@link EntityCreatingEvent} {@link EntityCreatedEvent}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link #patchUpdate(Object, Object)}</td>
 *         <td>{@link EntityUpdatingEvent} {@link EntityUpdatedEvent}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link #fullUpdate(Object)}</td>
 *         <td>{@link EntityUpdatingEvent} {@link EntityUpdatedEvent}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link #deleteById(Object)}</td>
 *         <td>{@link EntityDeletingEvent} {@link EntityDeletedEvent}</td>
 *     </tr>
 *     <tr>
 *         <td>{@link #softDeleteById(Object)}</td>
 *         <td>{@link EntitySoftDeletingEvent} {@link EntitySoftDeletedEvent}</td>
 *     </tr>
 * </table>
 *
 * @author EgorKor
 * @version 1.0
 * @implSpec Обязательно реализовать метод
 * {@link #getPersistenceAnnotatedEntityManager()}
 * предварительно помеченный аннотацией @PersistenceContext в классе наследнике.
 * Пример корректного наследования класса:
 * <pre>
 *     {@code
 * @Service
 * public class UserServiceImpl extends JpaCrudService<User, Long> implements UserService {
 *     @PersistenceContext
 *     private EntityManager entityManager;
 *
 *     @Autowired
 *     public UserServiceImpl(JpaRepository<User, Long> jpaRepository, JpaSpecificationExecutor<User> jpaSpecificationExecutor, ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate) {
 *         super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate);
 *     }
 *
 *     @Override
 *     public EntityManager getPersistenceAnnotatedEntityManager() {
 *         return entityManager;
 *     }
 * }}
 * </pre>
 * @see jakarta.persistence.PersistenceContext
 * @see io.github.egorkor.webutils.annotations.SoftDeleteFlag
 * @see io.github.egorkor.webutils.event.crud
 * @see org.springframework.context.event.EventListener
 * @since 2025
 */
public abstract class JpaCrudService<T, ID> implements CrudService<T, ID>, InitializingBean {

    private static final Set<Class<?>> SUPPORTED_SOFT_DELETE_TYPES = Set.of(
            Boolean.class, boolean.class,
            Timestamp.class, LocalDateTime.class, LocalDate.class, LocalTime.class,
            Instant.class, OffsetDateTime.class, OffsetTime.class, Date.class
    );
    private static final Map<Class<?>, Supplier<Object>> SOFT_DELETE_FLAG_MAPPING
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
    private static final Map<Class<?>, Supplier<Object>> RESTORE_FLAG_MAPPING = Map.of(
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

    protected final JpaRepository<T, ID> jpaRepository;
    protected final JpaSpecificationExecutor<T> jpaSpecificationExecutor;
    protected final ApplicationEventPublisher eventPublisher;
    protected final TransactionTemplate transactionTemplate;
    protected final Class<T> entityType;
    @Setter
    protected EntityManager entityManager;
    protected boolean isSoftDeleteSupported = false;
    protected Field softDeleteField;
    protected Field idField;

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
        defineIdField();
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
            this.softDeleteField.setAccessible(true);
        }
    }

    private void defineIdField() {
        this.idField = Arrays.stream(entityType.getDeclaredFields())
                .filter((f) -> f.isAnnotationPresent(Id.class)
                        || f.isAnnotationPresent(org.springframework.data.annotation.Id.class))
                .findAny().orElseThrow(
                        () -> new IllegalStateException("Entity " + entityType.getName() + " has no @Id field")
                );
    }

    private Filter<T> getSoftDeleteSupportedFilter(@NonNull Filter<T> filter) {
        if (!isSoftDeleteSupported) {
            return filter;
        }
        boolean isDeleted = false;
        Filter<T> softDeleteFilter = Filter.softDeleteFilter(softDeleteField, isDeleted);
        Filter<T> concantinatedFilter = filter.concat(softDeleteFilter);
        concantinatedFilter.setEntityType(entityType);
        return concantinatedFilter;
    }

    protected String getEntityTypeName() {
        return entityType == null ? "" : entityType.getSimpleName();
    }

    @Override
    public PageableResult<T> getAll(@NonNull Filter<T> filter,
                                    @NonNull Sorting sorting,
                                    @NonNull Pagination pagination) {
        filter.setEntityType(entityType);
        return PageableResult.of(jpaSpecificationExecutor.findAll(getSoftDeleteSupportedFilter(filter),
                pagination.toJpaPageable(sorting)));
    }

    @Override
    public T getById(@NonNull ID id) throws ResourceNotFoundException {
        Supplier<ResourceNotFoundException> exceptionSupplier = () ->
                new ResourceNotFoundException("Entity "
                        + getEntityTypeName()
                        + " with id = "
                        + id
                        + " not found.");
        boolean isDeleted = false;
        Filter<T> idFilter = Filter.builder().equals(idField.getName(), id.toString()).build();
        idFilter.setEntityType(entityType);
        return !isSoftDeleteSupported ?
                jpaRepository.findById(id)
                        .orElseThrow(exceptionSupplier) :
                jpaSpecificationExecutor.findOne(getSoftDeleteSupportedFilter(idFilter))
                        .orElseThrow(exceptionSupplier);
    }

    @Override
    public T getByIdWithFilter(@NonNull ID id,
                               @NonNull Filter<T> filter) throws ResourceNotFoundException {
        Supplier<ResourceNotFoundException> exceptionSupplier = () ->
                new ResourceNotFoundException("Entity "
                        + getEntityTypeName()
                        + " with id = "
                        + id
                        + " not found.");
        Filter<T> baseIdFilter = Filter.builder().equals(idField.getName(), id.toString()).build();
        Filter<T> resultIdFilter = getSoftDeleteSupportedFilter(baseIdFilter).concat(filter);
        resultIdFilter.setEntityType(entityType);
        return jpaSpecificationExecutor.findOne(resultIdFilter)
                .orElseThrow(exceptionSupplier);
    }

    @Override
    public T getByFilter(@NonNull Filter<T> filter) throws ResourceNotFoundException {
        Supplier<ResourceNotFoundException> exceptionSupplier = () ->
                new ResourceNotFoundException("Entity "
                        + getEntityTypeName()
                        + " with condition: "
                        + filter.toSQLFilter().replace("WHERE", "").trim()
                        + " not found.");
        filter.setEntityType(entityType);
        boolean isDeleted = false;
        return isSoftDeleteSupported ?
                jpaSpecificationExecutor.findOne(filter)
                        .orElseThrow(exceptionSupplier) :
                jpaSpecificationExecutor.findOne(
                                Filter.softDeleteFilter(softDeleteField, isDeleted)
                                        .concat(filter))
                        .orElseThrow(exceptionSupplier);
    }

    @Override
    public T getByIdWithLock(@NonNull ID id,
                             @NonNull LockModeType lockType) throws ResourceNotFoundException {
        Filter<T> idFilter = Filter.builder().equals(idField.getName(), id.toString()).build();
        idFilter.setEntityType(entityType);
        return getByFilterWithLock(idFilter, lockType);
    }

    @Override
    public T getByFilterWithLock(@NonNull Filter<T> filter,
                                 @NonNull LockModeType lockType) throws ResourceNotFoundException {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityType);
        Root<T> root = cq.from(entityType);
        filter.setEntityType(entityType);
        cq.select(root);
        cq.where(getSoftDeleteSupportedFilter(filter).toPredicate(root, cq, cb));
        TypedQuery<T> typedQuery = entityManager.createQuery(cq);
        typedQuery.setLockMode(lockType);
        return transactionTemplate.execute(status -> {
            try {
                return typedQuery.getSingleResult();
            } catch (NoResultException e) {
                throw new ResourceNotFoundException("Entity "
                        + getEntityTypeName()
                        + " with condition: "
                        + filter.toSQLFilter().replace("WHERE", "").trim()
                        + " not found.");
            }
        });
    }


    @Override
    public T create(@NonNull T model) throws EntityProcessingException {
        try {
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityCreatingEvent<>(this, model));
            }

            T saved = transactionTemplate.execute(status -> {
                try {
                    entityManager.persist(model);
                    return model;
                } catch (DataAccessException e) {
                    throw new EntityProcessingException("Entity saving data access error",
                            e, entityType, EntityOperation.CREATE);
                }
            });

            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityCreatedEvent<>(this, saved));
            }
            return saved;
        } catch (EntityProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected saving entity error",
                    e, entityType, EntityOperation.CREATE);
        }

    }

    @Override
    public T fullUpdate(@NonNull T model) throws EntityProcessingException {
        try {
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityUpdatingEvent<>(this, model));
            }
            T updated = transactionTemplate.execute(status -> {
                try {
                    return jpaRepository.save(model);
                } catch (DataAccessException e) {
                    throw new EntityProcessingException("Entity full updating data access error",
                            e, entityType, EntityOperation.CREATE);
                }
            });
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityUpdatedEvent<>(this, updated));
            }
            return updated;
        } catch (EntityProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected full updating entity error",
                    e, entityType, EntityOperation.CREATE);
        }
    }

    @Override
    public T patchUpdate(@NonNull ID id,
                         @NonNull T model) throws EntityProcessingException {
        try {
            T dbModel = getById(id);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityUpdatingEvent<>(this, dbModel));
            }
            JpaEntityPropertyPatcher.patch(model, dbModel);
            T updated = transactionTemplate.execute(status -> {
                try {
                    return jpaRepository.save(model);
                } catch (DataAccessException e) {
                    throw new EntityProcessingException("Entity patch updating data access error",
                            e, entityType, EntityOperation.CREATE);
                }
            });
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityUpdatedEvent<>(this, updated));
            }
            return updated;
        } catch (EntityProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected patch updating entity error",
                    e, entityType, EntityOperation.CREATE);
        }
    }

    @Override
    public void deleteById(@NonNull ID id) throws ResourceNotFoundException, EntityProcessingException {
        try {
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityDeletingEvent<>(this, id));
            }
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    jpaRepository.deleteById(id);
                } catch (DataAccessException e) {
                    throw new EntityProcessingException("Entity delete by id data access error: " + id, e, entityType, EntityOperation.DELETE);
                }
            });
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityDeletedEvent<>(this, id, entityType));
            }
        } catch (EntityProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected delete by id entity error: " + id,
                    e, entityType, EntityOperation.DELETE);
        }

    }

    @Override
    public void deleteAll() throws EntityProcessingException {
        try {
            jpaRepository.deleteAll();
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected delete all entities error", e, entityType, EntityOperation.DELETE);
        }
    }

    @Override
    public void deleteByFilter(@NonNull Filter<T> filter) throws EntityProcessingException {
        try {
            filter.setEntityType(entityType);
            jpaSpecificationExecutor.delete(filter);
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected delete by filter entities error: " + filter, e, entityType, EntityOperation.DELETE);
        }
    }

    @Override
    public long countByFilter(@NonNull Filter<T> filter) {
        filter.setEntityType(entityType);
        return jpaSpecificationExecutor.count(getSoftDeleteSupportedFilter(filter));
    }

    @Override
    public long countAll() {
        return !isSoftDeleteSupported ? jpaRepository.count() :
                countByFilter(getSoftDeleteSupportedFilter(Filter.emptyFilter()));
    }

    @Override
    public boolean existsById(@NonNull ID id) {
        return !isSoftDeleteSupported ? jpaRepository.existsById(id) :
                existsByFilter(Filter.builder().equals(idField.getName(),id.toString()).build());
    }

    @Override
    public boolean existsByFilter(@NonNull Filter<T> filter) {
        filter.setEntityType(entityType);
        return jpaSpecificationExecutor.exists(getSoftDeleteSupportedFilter(filter));
    }

    private void checkSoftDeleteAvailability() {
        if (!isSoftDeleteSupported) {
            throw new SoftDeleteUnsupportedException("Soft operation delete is not supported");
        }
    }

    @SneakyThrows
    @Override
    public void softDeleteById(@NonNull ID id) throws ResourceNotFoundException, SoftDeleteUnsupportedException, EntityProcessingException {
        checkSoftDeleteAvailability();
        T entity = getById(id);
        softDeleteField.set(entity, SOFT_DELETE_FLAG_MAPPING.get(softDeleteField.getType()).get());
        try {
            transactionTemplate.execute(status -> jpaRepository.save(entity));
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected soft delete entity by id error: " + id, e, entityType, EntityOperation.UPDATE);
        }
    }

    @Override
    public void softDeleteAll() throws SoftDeleteUnsupportedException, EntityProcessingException {
        softDeleteByFilter(Filter.emptyFilter(entityType));
    }

    @Override
    public void softDeleteByFilter(@NonNull Filter<T> filter) throws SoftDeleteUnsupportedException, EntityProcessingException {
        checkSoftDeleteAvailability();
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaUpdate<T> update = cb.createCriteriaUpdate(entityType);
            Root<T> root = update.from(entityType);
            update.set(root.get(softDeleteField.getName()),
                    SOFT_DELETE_FLAG_MAPPING.get(softDeleteField.getType()).get());
            if (filter.isFiltered()) {
                filter.setEntityType(entityType);
                update.where(filter.toPredicate(root,cb));
            }
            transactionTemplate.executeWithoutResult(status -> {
                entityManager.createQuery(update).executeUpdate();
            });

        } catch (Exception e) {
            throw new EntityProcessingException(
                    "Unexpected soft delete entities by filter error: " + filter,
                    e,
                    entityType,
                    EntityOperation.UPDATE
            );
        }
    }

    @SneakyThrows
    @Override
    public void restoreById(@NonNull ID id) throws ResourceNotFoundException, SoftDeleteUnsupportedException, EntityProcessingException {
        checkSoftDeleteAvailability();
        T entity = getById(id);
        softDeleteField.set(entity, RESTORE_FLAG_MAPPING.get(softDeleteField.getType()).get());
        try {
            jpaRepository.save(entity);
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected restore entity by id error: " + id, e, entityType, EntityOperation.UPDATE);
        }
    }

    @Override
    public void restoreAll() throws SoftDeleteUnsupportedException, EntityProcessingException {
        restoreByFilter(Filter.emptyFilter(entityType));
    }

    @Override
    public void restoreByFilter(@NonNull Filter<T> filter) throws SoftDeleteUnsupportedException, EntityProcessingException {
        checkSoftDeleteAvailability();
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaUpdate<T> update = cb.createCriteriaUpdate(entityType);
            Root<T> root = update.from(entityType);
            update.set(root.get(softDeleteField.getName()),
                    RESTORE_FLAG_MAPPING.get(softDeleteField.getType()).get());
            if (filter.isFiltered()) {
                filter.setEntityType(entityType);
                update.where(filter.toPredicate(root, cb));
            }
            transactionTemplate.executeWithoutResult(status -> {
                entityManager.createQuery(update).executeUpdate();
            });
        } catch (Exception e) {
            throw new EntityProcessingException(
                    "Unexpected restore entity by filter error: " + filter,
                    e,
                    entityType,
                    EntityOperation.UPDATE
            );
        }
    }

}
