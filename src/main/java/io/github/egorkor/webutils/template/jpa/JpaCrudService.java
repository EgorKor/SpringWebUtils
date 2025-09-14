package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.annotations.SoftDeleteFlag;
import io.github.egorkor.webutils.event.crud.*;
import io.github.egorkor.webutils.exception.*;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import io.github.egorkor.webutils.service.CrudService;
import io.github.egorkor.webutils.service.PageableResult;
import io.github.egorkor.webutils.service.UpdateSpecification;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.jpa.HibernateHints;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
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
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.github.egorkor.webutils.queryparam.Filter.fb;


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
    protected final Validator validator;
    protected final Class<T> entityType;
    @Setter
    protected EntityManager entityManager;
    protected boolean isSoftDeleteSupported = false;
    protected Field softDeleteField;
    protected Field idField;

    public JpaCrudService(JpaRepository<T, ID> jpaRepository,
                          JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                          ApplicationEventPublisher eventPublisher,
                          TransactionTemplate transactionTemplate,
                          Validator validator) {
        this.jpaRepository = jpaRepository;
        this.jpaSpecificationExecutor = jpaSpecificationExecutor;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.validator = validator;

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

    @Override
    public List<T> getList() {
        return getList(Filter.empty());
    }

    @Override
    public Stream<T> getDataStream() {
        return getDataStream(Filter.empty());
    }

    @Override
    public PageableResult<T> getPage(Filter<T> filter, Pagination pagination) {
        filter.setEntityType(entityType);
        return getPage(filter, Sorting.unsorted(), pagination);
    }

    @Override
    public List<T> getList(Filter<T> filter, Sorting sorting) {
        filter.setEntityType(entityType);
        return jpaSpecificationExecutor.findAll(getSoftDeleteSupportedFilter(filter), sorting.toJpaSort());
    }

    @Override
    public List<T> getList(Filter<T> filter) {
        filter.setEntityType(entityType);
        return jpaSpecificationExecutor.findAll(getSoftDeleteSupportedFilter(filter));
    }

    @Override
    public Stream<T> getDataStream(Filter<T> filter) {
        filter.setEntityType(entityType);
        return getDataStream(filter, Sorting.unsorted());
    }

    @Override
    public Stream<T> getDataStream(Filter<T> filter, Sorting sorting) {
        filter.setEntityType(entityType);
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = cb.createQuery(entityType);
        Root<T> root = criteriaQuery.from(entityType);
        criteriaQuery.select(root);
        criteriaQuery
                .where(getSoftDeleteSupportedFilter(filter)
                        .toPredicate(root, cb));
        criteriaQuery.orderBy(sorting.toCriteriaOrderList(root, cb));
        TypedQuery<T> typedQuery = entityManager.createQuery(criteriaQuery);
        return typedQuery
                .setHint(HibernateHints.HINT_BATCH_FETCH_SIZE, "100")
                .setHint(HibernateHints.HINT_READ_ONLY, true)
                .getResultStream();
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
        this.idField.setAccessible(true);
    }

    private Filter<T> getSoftDeleteSupportedFilter(@NonNull Filter<T> filter) {
        if (!isSoftDeleteSupported) {
            return filter;
        }
        boolean isDeleted = false;
        Filter<T> softDeleteFilter = Filter.softDeleteFilter(softDeleteField, isDeleted);
        Filter<T> concantinatedFilter = filter._and(softDeleteFilter);
        concantinatedFilter.setEntityType(entityType);
        return concantinatedFilter;
    }

    protected String getEntityTypeName() {
        return entityType == null ? "" : entityType.getSimpleName();
    }

    @Override
    public PageableResult<T> getPage(@NonNull Filter<T> filter,
                                     @NonNull Sorting sorting,
                                     @NonNull Pagination pagination) {
        filter.setEntityType(entityType);
        Filter<T> countFilter = new Filter<>(filter.getFilter());
        return PageableResult.of(jpaSpecificationExecutor.findAll(getSoftDeleteSupportedFilter(filter),
                getSoftDeleteSupportedFilter(countFilter),
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
        Filter<T> idFilter = fb.and(fb.equals(idField.getName(), id.toString())).build();
        idFilter.setEntityType(entityType);
        return !isSoftDeleteSupported ?
                jpaRepository.findById(id)
                        .orElseThrow(exceptionSupplier) :
                jpaSpecificationExecutor.findOne(getSoftDeleteSupportedFilter(idFilter))
                        .orElseThrow(exceptionSupplier);
    }

    @Override
    public T getById(@NonNull ID id,
                     @NonNull String... fetchingProperties) throws ResourceNotFoundException {
        Supplier<ResourceNotFoundException> exceptionSupplier = () ->
                new ResourceNotFoundException("Entity "
                        + getEntityTypeName()
                        + " with id = "
                        + id
                        + " not found.");
        Filter<T> baseIdFilter = fb.and(fb.equals(idField.getName(), id.toString())).build();
        Filter<T> resultIdFilter = getSoftDeleteSupportedFilter(baseIdFilter);
        Arrays.stream(fetchingProperties).forEach(resultIdFilter::withFetchJoin);

        resultIdFilter.setEntityType(entityType);
        return jpaSpecificationExecutor.findOne(resultIdFilter)
                .orElseThrow(exceptionSupplier);
    }

    @Override
    public T getByFilter(@NonNull Filter<T> filter) throws ResourceNotFoundException, NonUniqueResultException {
        Supplier<ResourceNotFoundException> exceptionSupplier = () ->
                new ResourceNotFoundException("Entity "
                        + getEntityTypeName()
                        + " with condition: "
                        + filter.toSQLFilter().replace("WHERE", "").trim()
                        + " not found.");
        filter.setEntityType(entityType);
        boolean isDeleted = false;
        return !isSoftDeleteSupported ?
                jpaSpecificationExecutor.findOne(filter)
                        .orElseThrow(exceptionSupplier) :
                jpaSpecificationExecutor.findOne(getSoftDeleteSupportedFilter(filter))
                        .orElseThrow(exceptionSupplier);
    }

    @Override
    public T getByIdWithLock(@NonNull ID id,
                             @NonNull LockModeType lockType) throws ResourceNotFoundException {
        Filter<T> idFilter = fb.and(fb.equals(idField.getName(), id.toString())).build();
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
        Set<ConstraintViolation<T>> violations = validator.validate(model);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
        try {
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityCreatingEvent<>(this, model));
            }

            T saved = transactionTemplate.execute(status -> {
                try {
                    return jpaRepository.save(model);
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
        Set<ConstraintViolation<T>> violations = validator.validate(model);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
        try {
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityUpdatingEvent<>(this, model));
            }
            T updated = transactionTemplate.execute(status -> {
                try {
                    Session session = entityManager.unwrap(Session.class);
                    session.update(model);
                    return model;
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
        Set<ConstraintViolation<T>> violations = validator.validate(model);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
        try {
            T dbModel = getById(id);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityUpdatingEvent<>(this, dbModel));
            }
            JpaEntityPropertyPatcher.patch(model, dbModel);
            T updated = transactionTemplate.execute(status -> {
                try {
                    return jpaRepository.save(dbModel);
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
                    if (deleteByFilter(Filter.equals(idField.getName(), id)) != 1) {
                        throw new ResourceNotFoundException("Entity "
                                + getEntityTypeName()
                                + " with id = "
                                + id
                                + " not found.");
                    }
                } catch (DataAccessException e) {
                    throw new EntityProcessingException("Entity delete by id data access error: " + id, e, entityType, EntityOperation.DELETE);
                }
            });
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new EntityDeletedEvent<>(this, id, entityType));
            }
        } catch (EntityProcessingException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected delete by id entity error: " + id,
                    e, entityType, EntityOperation.DELETE);
        }

    }

    @Override
    public long deleteAll() throws EntityProcessingException {
        try {
            return deleteByFilter(Filter.empty(entityType));
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected delete all entities error", e, entityType, EntityOperation.DELETE);
        }
    }

    @Override
    public long deleteByFilter(@NonNull Filter<T> filter) throws EntityProcessingException {
        try {
            filter.setEntityType(entityType);
            return jpaSpecificationExecutor.delete(filter);
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
                countByFilter(getSoftDeleteSupportedFilter(Filter.empty()));
    }

    @Override
    public boolean existsById(@NonNull ID id) {
        return !isSoftDeleteSupported ? jpaRepository.existsById(id) :
                existsByFilter(fb.and(fb.equals(idField.getName(), id.toString())).build());
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
        try {
            Object updateValue = SOFT_DELETE_FLAG_MAPPING.get(softDeleteField.getType()).get();
            transactionTemplate.executeWithoutResult(status -> {
                if (updateByFilter(
                        UpdateSpecification.updateValue(softDeleteField.getName(), updateValue),
                        Filter.equals(idField.getName(), id)) != 1) {
                    throw new ResourceNotFoundException("Entity "
                            + getEntityTypeName()
                            + " with id = "
                            + id
                            + " not found.");
                }
            });
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected soft delete entity by id error: " + id, e, entityType, EntityOperation.UPDATE);
        }
    }

    @Override
    public int softDeleteAll() throws SoftDeleteUnsupportedException, EntityProcessingException {
        return softDeleteByFilter(Filter.empty(entityType));
    }

    @Override
    public int updateByFilter(UpdateSpecification specification, Filter<T> filter) {
        filter.setEntityType(entityType);
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<T> update = cb.createCriteriaUpdate(entityType);
        Root<T> root = update.from(entityType);

        for (Map.Entry<String, UpdateSpecification.UpdatePair> entry :
                specification.getUpdates().entrySet()) {

            String field = entry.getKey();
            UpdateSpecification.UpdatePair pair = entry.getValue();
            Path<Object> path = root.get(field);

            switch (pair.action()) {
                case UPDATE -> update.set(path, pair.data());
                case SUM -> {
                    if (pair.data() instanceof Number number) {
                        Object sumExpr = cb.sum(Filter.getTypedExpression(path, Number.class), number);
                        update.set(path, sumExpr);
                    }
                }
                case MULTIPLY -> {
                    if (pair.data() instanceof Number number) {
                        Object prodExpr = cb.prod(Filter.getTypedExpression(path, Number.class), number);
                        update.set(path, prodExpr);
                    }
                }
                case DIVIDE -> {
                    if (pair.data() instanceof Number number) {
                        Object quotExpr = cb.quot(Filter.getTypedExpression(path, Number.class), number);
                        update.set(path, quotExpr);
                    }
                }
                case ADD_DAYS -> {
                    if (pair.data() instanceof Integer days) {
                        if (path.getJavaType() == LocalDate.class) {
                            Object dateAddExpr = cb.function(
                                    "DATE_ADD",
                                    LocalDate.class,
                                    path,
                                    cb.literal(days)
                            );
                            update.set(path, dateAddExpr);
                        } else if (path.getJavaType() == LocalDateTime.class) {
                            Object dateTimeAddExpr = cb.function(
                                    "DATE_ADD",
                                    LocalDateTime.class,
                                    path,
                                    cb.literal(days)
                            );
                            update.set(path, dateTimeAddExpr);
                        }
                    }
                }
                case TRUNCATE_TIME -> {
                    if (path.getJavaType() == LocalDateTime.class) {
                        Object truncExpr = cb.function(
                                "TRUNC",
                                LocalDate.class,
                                path
                        );
                        update.set(path, truncExpr);
                    }
                }
                case CONCAT -> {
                    if (pair.data() instanceof String value) {
                        Object concatExpr = cb.concat(path.as(String.class), value);
                        update.set(path, concatExpr);
                    }
                }
                case UPPER_CASE -> {
                    Object upperExpr = cb.upper(path.as(String.class));
                    update.set(path, upperExpr);
                }
                case LOWER_CASE -> {
                    Object lowerExpr = cb.lower(path.as(String.class));
                    update.set(path, lowerExpr);
                }
                case COPY -> {
                    if (pair.data() instanceof String sourceField) {
                        Object sourcePath = root.get(sourceField);
                        update.set(path, sourcePath);
                    }
                }
            }
        }
        update.where(filter.toPredicate(root, cb));
        return entityManager.createQuery(update).executeUpdate();
    }

    @Override
    public int softDeleteByFilter(@NonNull Filter<T> filter) throws SoftDeleteUnsupportedException, EntityProcessingException {
        checkSoftDeleteAvailability();
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaUpdate<T> update = cb.createCriteriaUpdate(entityType);
            Root<T> root = update.from(entityType);
            update.set(root.get(softDeleteField.getName()),
                    SOFT_DELETE_FLAG_MAPPING.get(softDeleteField.getType()).get());
            if (filter.isFiltered()) {
                filter.setEntityType(entityType);
                update.where(filter.toPredicate(root, cb));
            }
            return transactionTemplate.execute(status -> entityManager.createQuery(update).executeUpdate());
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
        Object updateValue = RESTORE_FLAG_MAPPING.get(softDeleteField.getType()).get();
        try {
            int updatedCount = updateByFilter(
                    UpdateSpecification.updateValue(softDeleteField.getName(), updateValue),
                    Filter.equals(idField.getName(), id)
            );
            if (updatedCount != 1) {
                throw new ResourceNotFoundException("Entity not found: " + id);
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityProcessingException("Unexpected restore entity by id error: " + id, e, entityType, EntityOperation.UPDATE);
        }
    }

    @Override
    public void restoreAll() throws SoftDeleteUnsupportedException, EntityProcessingException {
        restoreByFilter(Filter.empty(entityType));
    }

    @Override
    public void restoreByFilter(@NonNull Filter<T> filter) throws SoftDeleteUnsupportedException, EntityProcessingException, ResourceNotFoundException {
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
