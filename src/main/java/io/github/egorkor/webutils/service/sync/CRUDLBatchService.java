package io.github.egorkor.webutils.service.sync;

import io.github.egorkor.webutils.service.batching.BatchResult;
import io.github.egorkor.webutils.service.batching.BatchResultWithData;

import java.util.List;

public interface CRUDLBatchService<T, ID> extends CRUDLService<T, ID> {
    List<BatchResultWithData<T>> batchCreate(List<T> models);

    List<BatchResult> batchUpdate(List<T> models);

    List<BatchResult> batchDelete(List<ID> ids);

    List<T> batchCreateAtomic(List<T> models);

    List<T> batchUpdateAtomic(List<T> models);

    void batchDeleteAtomic(List<ID> ids);

    List<BatchResultWithData<T>> batchCreate(List<T> models, int batchSize);

    List<BatchResult> batchUpdate(List<T> models, int batchSize);

    List<BatchResult> batchDelete(List<ID> ids, int batchSize);

    List<T> batchCreateAtomic(List<T> models, int batchSize);

    List<T> batchUpdateAtomic(List<T> models, int batchSize);

    void batchDeleteAtomic(List<ID> ids, int batchSize);

}
