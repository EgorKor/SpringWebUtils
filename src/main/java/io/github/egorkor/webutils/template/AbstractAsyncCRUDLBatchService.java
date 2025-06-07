package io.github.egorkor.webutils.template;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.scheduling.annotation.Async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Transactional
public class AbstractAsyncCRUDLBatchService<T, ID> extends AbstractAsyncCRUDLService<T, ID>
        implements AsyncCRUDLBatchService<T, ID> {

    public AbstractAsyncCRUDLBatchService(JpaRepository<T, ID> jpaRepository, JpaSpecificationExecutor<T> jpaSpecificationExecutor) {
        super(jpaRepository, jpaSpecificationExecutor);
    }

    @Async
    @Override
    public CompletableFuture<List<BatchResultWithData<T>>> batchCreateAsync(List<T> models) {
        return CompletableFuture.supplyAsync(() -> {
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
                if (++counter == 50) {
                    jpaRepository.flush();
                }
            }
            return results;
        });
    }

    @Async
    @Override
    public CompletableFuture<List<BatchResult>> batchUpdateAsync(List<T> models) {
        return CompletableFuture.supplyAsync(() -> {
            List<BatchResult> results = new ArrayList<>();

            int counter = 0;
            for (T model : models) {
                try {
                    model = jpaRepository.save(model);
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
        });
    }

    @Async
    @Override
    public CompletableFuture<List<BatchResult>> batchDeleteAsync(List<ID> ids) {
        return CompletableFuture.supplyAsync(() -> {
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
        });

    }
}
