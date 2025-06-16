package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.exception.BatchOperationException;
import io.github.egorkor.webutils.service.batching.BatchOperationStatus;
import io.github.egorkor.webutils.service.batching.BatchResult;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;
import io.github.egorkor.webutils.service.sync.CRUDLBatchService;
import io.github.egorkor.webutils.template.BatchResultWithDataImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JpaBatchService<T, ID> extends JpaService<T, ID> implements CRUDLBatchService<T, ID> {
    protected final TransactionTemplate transactionTemplate;
    protected final EntityManager entityManager;

    private static final int DEFAULT_BATCH_SIZE = 100;

    public JpaBatchService(JpaRepository<T, ID> jpaRepository,
                           JpaSpecificationExecutor<T> jpaSpecificationExecutor,
                           TransactionTemplate template,
                           EntityManager entityManager) {
        super(jpaRepository, jpaSpecificationExecutor);
        this.transactionTemplate = template;
        this.entityManager = entityManager;

    }

    @Override
    public List<BatchResultWithData<T>> batchCreate(List<T> models) {
        return batchCreate(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public List<BatchResult> batchUpdate(List<T> models) {
        return batchUpdate(models, DEFAULT_BATCH_SIZE);
    }

    @Override
    public List<BatchResult> batchDelete(List<ID> ids) {
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
        return transactionTemplate.execute(status -> {
            List<BatchResultWithData<T>> results = new ArrayList<>();
            int counter = 0;
            for (T model : models) {
                try {
                    model = this.create(model);
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
                    jpaRepository.flush();
                    entityManager.clear();
                }
            }
            return results;
        });
    }

    @Override
    public List<BatchResult> batchUpdate(List<T> models, int batchSize) {
        return transactionTemplate.execute(status -> {
            List<BatchResult> results = new ArrayList<>();
            int counter = 0;
            for (T model : models) {
                try {
                    model = this.update(model);
                    BatchResultWithDataImpl<T> result = BatchResultWithDataImpl
                            .<T>builder()
                            .data(model)
                            .status(BatchOperationStatus.SUCCESS)
                            .message("updated")
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
                    jpaRepository.flush();
                    entityManager.clear();
                }
            }
            return results;
        });
    }

    @Override
    public List<BatchResult> batchDelete(List<ID> ids, int batchSize) {
        return transactionTemplate.execute(status -> {
            List<BatchResult> results = new ArrayList<>();
            int counter = 0;
            for (ID id : ids) {
                try {
                    jpaRepository.deleteById(id);
                    BatchResultWithDataImpl result = BatchResultWithDataImpl.builder()
                            .message("deleted")
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
                    jpaRepository.flush();
                    entityManager.clear();
                }
            }
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
                    results.add(this.create(model));
                } catch (Exception e) {
                    log.error("create operation fails for entity: {} \ncause: {}", model.toString(), e.getMessage());
                    status.setRollbackOnly();
                    throw new BatchOperationException(e.getMessage());
                }
                if (++counter % batchSize == 0) {
                    jpaRepository.flush();
                    entityManager.clear();
                }
            }
            return results;
        });
    }

    @Override
    public List<T> batchUpdateAtomic(List<T> models, int batchSize) {
        return transactionTemplate.execute(status -> {
            List<T> results = new ArrayList<>();
            int counter = 0;
            for (T model : models) {
                try {
                    results.add(this.create(model));
                } catch (Exception e) {
                    log.error("update operation fails for entity: {} \ncause: {}",
                            model.toString(),
                            e.getMessage());
                    status.setRollbackOnly();
                    throw new BatchOperationException(e.getMessage());
                }
                if (++counter % batchSize == 0) {
                    jpaRepository.flush();
                    entityManager.clear();
                }
            }
            return results;
        });

    }

    @Override
    public void batchDeleteAtomic(List<ID> ids, int batchSize) {
        transactionTemplate.executeWithoutResult(status -> {
            int counter = 0;
            for (ID id : ids) {
                try {
                    jpaRepository.deleteById(id);
                } catch (Exception e) {
                    log.error("delete operation fails for entity with id: {} \ncause: {}",
                            id,
                            e.getMessage());
                    status.setRollbackOnly();
                    throw new BatchOperationException(e.getMessage());
                }
                if (++counter % batchSize == 0) {
                    jpaRepository.flush();
                    entityManager.clear();
                }
            }
        });

    }
}
