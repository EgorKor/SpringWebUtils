package io.github.egorkor.webutils.template;

import io.github.egorkor.webutils.service.batching.BatchOperationStatus;
import io.github.egorkor.webutils.service.batching.BatchResult;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;
import io.github.egorkor.webutils.service.sync.CRUDLBatchService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Transactional
public class JpaBatchService<T, ID> extends JpaService<T, ID> implements CRUDLBatchService<T, ID> {
    public JpaBatchService(JpaRepository<T, ID> jpaRepository, JpaSpecificationExecutor<T> jpaSpecificationExecutor) {
        super(jpaRepository, jpaSpecificationExecutor);
    }

    @Override
    public List<BatchResultWithData<T>> batchCreate(List<T> models) {
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
            if (++counter == 50) {
                jpaRepository.flush();
            }
        }
        return results;
    }

    @Override
    public List<BatchResult> batchUpdate(List<T> models) {
        List<BatchResult> results = new ArrayList<>();
        int counter = 0;
        for (T model : models) {
            try {
                model = this.create(model);
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
            if (++counter == 50) {
                jpaRepository.flush();
            }
        }
        return results;
    }

    @Override
    public List<BatchResult> batchDelete(List<ID> ids) {
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
            if (++counter == 50) {
                jpaRepository.flush();
            }
        }
        return results;
    }

    @Override
    public List<T> batchCreateAtomic(List<T> models) {
        List<T> results = new ArrayList<>();
        int counter = 0;
        for (T model : models) {
            try {
                results.add(this.create(model));
            } catch (Exception e) {
                log.error("create operation fails for entity: {} \ncause: {}", model.toString(), e.getMessage());
                throw new RuntimeException(e);
            }
            if (++counter == 50) {
                jpaRepository.flush();
            }
        }
        return results;
    }

    @Override
    public List<T> batchUpdateAtomic(List<T> models) {
        List<T> results = new ArrayList<>();
        int counter = 0;
        for (T model : models) {
            try {
                results.add(this.create(model));
            } catch (Exception e) {
                log.error("update operation fails for entity: {} \ncause: {}",
                        model.toString(),
                        e.getMessage());
                throw new RuntimeException(e);
            }
            if (++counter == 50) {
                jpaRepository.flush();
            }
        }
        return results;
    }

    @Override
    public void batchDeleteAtomic(List<ID> ids) {
        int counter = 0;
        for (ID id : ids) {
            try {
                jpaRepository.deleteById(id);
            } catch (Exception e) {
                log.error("delete operation fails for entity with id: {} \ncause: {}",
                        id,
                        e.getMessage());
                throw new RuntimeException(e);
            }
            if (++counter == 50) {
                jpaRepository.flush();
            }
        }
    }
}
