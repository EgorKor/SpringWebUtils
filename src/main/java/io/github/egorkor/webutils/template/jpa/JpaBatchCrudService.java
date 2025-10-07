package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.exception.BatchOperationException;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.exception.ValidationException;
import io.github.egorkor.webutils.service.CrudBatchService;
import io.github.egorkor.webutils.service.batching.BatchOperationStatus;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;
import io.github.egorkor.webutils.template.BatchResultWithDataImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Slf4j
public abstract class JpaBatchCrudService<T, ID>
        extends JpaCrudService<T, ID>
        implements CrudBatchService<T, ID> {
    private static final int DEFAULT_BATCH_SIZE = 100;


    @Deprecated(since = "1.0.3")
    public JpaBatchCrudService(JpaRepository<T, ID> jpaRepository,
                               JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                               ApplicationEventPublisher eventPublisher,
                               TransactionTemplate transactionTemplate,
                               Validator validator) {
        super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate, validator);
    }

    public JpaBatchCrudService(JpaRepository<T, ID> jpaRepository,
                               JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                               TransactionTemplate transactionTemplate,
                               Validator validator) {
        super(jpaRepository, jpaSpecificationExecutor, transactionTemplate, validator);
    }


    @Override
    public List<BatchResultWithData<T>> batchCreate(List<T> models) {
        return batchCreate(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public List<BatchResultWithData<ID>> batchDelete(List<ID> ids) {
        return batchDelete(ids, DEFAULT_BATCH_SIZE);
    }

    @Override
    public List<T> batchCreateAtomic(List<T> models) {
        return batchCreateAtomic(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public void batchDeleteAtomic(List<ID> ids) {
        batchDeleteAtomic(ids, DEFAULT_BATCH_SIZE);
    }

    @Override
    public List<BatchResultWithData<T>> batchCreate(List<T> models, int batchSize) {
        return transactionTemplate.execute(status -> {
            List<BatchResultWithData<T>> results = new ArrayList<>();
            int counter = 0;
            for (T model : models) {
                try {
                    Set<ConstraintViolation<T>> violations = validator.validate(model);
                    if (!violations.isEmpty()) {
                        throw new ValidationException("Ошибка валидации сущности " + getEntityTypeName(),violations);
                    }
                    model = jpaRepository.save(model);
                    BatchResultWithDataImpl<T> result = BatchResultWithDataImpl
                            .<T>builder()
                            .data(model)
                            .status(BatchOperationStatus.SUCCESS)
                            .message("created")
                            .build();
                    results.add(result);
                } catch (Exception e) {
                    BatchResultWithDataImpl<T> result = BatchResultWithDataImpl
                            .<T>builder()
                            .status(BatchOperationStatus.FAILED)
                            .message("create operation fails for entity: " + model.toString())
                            .details(e.getMessage())
                            .build();
                    results.add(result);

                }
                if (++counter % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            entityManager.flush();
            entityManager.clear();
            return results;
        });
    }


    @Override
    public List<BatchResultWithData<ID>> batchDelete(List<ID> ids, int batchSize) {
        return transactionTemplate.execute(status -> {
            List<BatchResultWithData<ID>> results = new ArrayList<>();
            int counter = 0;
            for (ID id : ids) {
                try {
                    if (!existsById(id)) {
                        throw new ResourceNotFoundException(getResourceNotFoundMessage(id));
                    }
                    jpaRepository.deleteById(id);
                    entityManager.flush();
                    BatchResultWithDataImpl result = BatchResultWithDataImpl.builder()
                            .message("deleted")
                            .data(id)
                            .status(BatchOperationStatus.SUCCESS)
                            .build();
                    results.add(result);
                } catch (Exception e) {
                    BatchResultWithDataImpl result = BatchResultWithDataImpl.builder()
                            .message("delete operation fails for entity with id: " + id.toString())
                            .status(BatchOperationStatus.FAILED)
                            .details(e.getMessage())
                            .build();
                    results.add(result);
                }
                if (++counter % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            entityManager.flush();
            entityManager.clear();
            return results;
        });
    }

    @Override
    public List<T> batchCreateAtomic(List<T> models, int batchSize) {
        return transactionTemplate.execute((status) -> {
            List<T> results = new ArrayList<>();
            int counter = 0;
            for (T model : models) {
                try {
                    Set<ConstraintViolation<T>> violations = validator.validate(model);
                    if (!violations.isEmpty()) {
                        throw new ValidationException("Ошибка валидации сущности " + getEntityTypeName(), violations);
                    }
                    model = jpaRepository.save(model);
                    results.add(model);
                } catch (Exception e) {
                    log.error("create operation fails for entity: {} \ncause: {}", model.toString(), e.getMessage(), e);
                    status.setRollbackOnly();
                    throw new BatchOperationException(e.getMessage());
                }
                if (++counter % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            entityManager.flush();
            entityManager.clear();
            return results;
        });
    }

    @Override
    public void batchDeleteAtomic(List<ID> ids, int batchSize) {
        transactionTemplate.executeWithoutResult(status -> {
            int counter = 0;
            for (ID id : ids) {
                try {
                    if (!existsById(id)) {
                        throw new ResourceNotFoundException(getResourceNotFoundMessage(id));
                    }
                    jpaRepository.deleteById(id);
                } catch (Exception e) {
                    log.error("delete operation fails for entity with id: {} \ncause: {}",
                            id,
                            e.getMessage(),
                            e);
                    status.setRollbackOnly();
                    throw new BatchOperationException(e.getMessage());
                }
                if (++counter % batchSize == 0) {
                    entityManager.clear();
                    entityManager.flush();
                }
            }
            entityManager.flush();
            entityManager.clear();
        });
    }

}
