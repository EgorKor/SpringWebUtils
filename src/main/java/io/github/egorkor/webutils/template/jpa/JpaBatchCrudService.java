package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.event.batching.*;
import io.github.egorkor.webutils.exception.BatchOperationException;
import io.github.egorkor.webutils.service.batching.BatchOperationStatus;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;
import io.github.egorkor.webutils.service.sync.CrudBatchService;
import io.github.egorkor.webutils.template.BatchResultWithDataImpl;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;


/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Slf4j
public abstract class JpaBatchCrudService<T, ID>
        extends JpaCrudService<T, ID>
        implements CrudBatchService<T, ID>, InitializingBean {
    private static final int DEFAULT_BATCH_SIZE = 100;

    private EntityManager entityManager;

    public JpaBatchCrudService(JpaRepository<T, ID> jpaRepository,
                               JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                               ApplicationEventPublisher eventPublisher,
                               TransactionTemplate transactionTemplate) {
        super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate);
    }


    @Override
    public List<BatchResultWithData<T>> batchCreate(List<T> models) {
        return batchCreate(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public List<BatchResultWithData<T>> batchUpdate(List<T> models) {
        return batchUpdate(models, DEFAULT_BATCH_SIZE);
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
    public List<T> batchUpdateAtomic(List<T> models) {
        return batchUpdateAtomic(models, DEFAULT_BATCH_SIZE);

    }

    @Override
    public void batchDeleteAtomic(List<ID> ids) {
        batchDeleteAtomic(ids, DEFAULT_BATCH_SIZE);
    }

    @Override
    public List<BatchResultWithData<T>> batchCreate(List<T> models, int batchSize) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchCreatingEvent<>(this, models));
        }
        var batchResult = transactionTemplate.execute(status -> {
            List<BatchResultWithData<T>> results = new ArrayList<>();
            int counter = 0;
            for (T model : models) {
                try {
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
            return results;
        });
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchCreatedEvent<>(this, batchResult));
        }
        return batchResult;
    }

    @Override
    public List<BatchResultWithData<T>> batchUpdate(List<T> models, int batchSize) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchUpdatingEvent(this, models));
        }
        var batchResult = transactionTemplate.execute(status -> {
            List<BatchResultWithData<T>> results = new ArrayList<>();
            int counter = 0;
            for (T model : models) {
                try {
                    model = jpaRepository.save(model);
                    BatchResultWithDataImpl<T> result = BatchResultWithDataImpl
                            .<T>builder()
                            .data(model)
                            .status(BatchOperationStatus.SUCCESS)
                            .message("updated")
                            .data(model)
                            .build();
                    results.add(result);
                } catch (Exception e) {
                    BatchResultWithDataImpl<T> result = BatchResultWithDataImpl
                            .<T>builder()
                            .status(BatchOperationStatus.FAILED)
                            .message("update operation fails for entity: " + model.toString())
                            .details(e.getMessage())
                            .build();
                    results.add(result);
                }
                if (++counter % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            return results;
        });
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchUpdatedEvent<>(this, batchResult));
        }
        return batchResult;
    }

    @Override
    public List<BatchResultWithData<ID>> batchDelete(List<ID> ids, int batchSize) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchDeletingEvent<>(this, ids, entityType));
        }
        var batchResult = transactionTemplate.execute(status -> {
            List<BatchResultWithData<ID>> results = new ArrayList<>();
            int counter = 0;
            for (ID id : ids) {
                try {
                    jpaRepository.deleteById(id);
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
            return results;
        });
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchDeletedEvent<>(this, batchResult, entityType));
        }
        return batchResult;
    }

    @Override
    public List<T> batchCreateAtomic(List<T> models, int batchSize) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchCreatingEvent<>(this, models));
        }
        var batchResults = transactionTemplate.execute((status) -> {
            List<T> results = new ArrayList<>();
            int counter = 0;
            for (T model : models) {
                try {
                    results.add(jpaRepository.save(model));
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
            return results;
        });
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchCreatedAtomicEvent<>(this, batchResults));
        }
        return batchResults;
    }

    @Override
    public List<T> batchUpdateAtomic(List<T> models, int batchSize) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchUpdatingEvent(this, models));
        }
        var batchResult = transactionTemplate.execute(status -> {
            List<T> results = new ArrayList<>();
            int counter = 0;
            for (T model : models) {
                try {
                    results.add(jpaRepository.save(model));
                } catch (Exception e) {
                    log.error("update operation fails for entity: {} \ncause: {}",
                            model.toString(),
                            e.getMessage(),
                            e);
                    status.setRollbackOnly();
                    throw new BatchOperationException(e.getMessage());
                }
                if (++counter % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            return results;
        });
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchUpdatedAtomicEvent<>(this, batchResult));
        }
        return batchResult;
    }

    @Override
    public void batchDeleteAtomic(List<ID> ids, int batchSize) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchDeletingEvent<>(this, ids, entityType));
        }
        transactionTemplate.executeWithoutResult(status -> {
            int counter = 0;
            for (ID id : ids) {
                try {
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
                    jpaRepository.flush();
                    entityManager.clear();
                }
            }
        });
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new BatchDeletedAtomicEvent<>(this, ids, entityType));
        }

    }

}
