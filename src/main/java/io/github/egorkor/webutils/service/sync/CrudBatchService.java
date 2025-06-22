package io.github.egorkor.webutils.service.sync;

import io.github.egorkor.webutils.service.batching.BatchResultWithData;

import java.util.List;

public interface CrudBatchService<T, ID> extends CrudService<T, ID> {
    List<BatchResultWithData<T>> batchCreate(List<T> models);

    List<BatchResultWithData<T>> batchUpdate(List<T> models);

    List<BatchResultWithData<ID>> batchDelete(List<ID> ids);

    List<T> batchCreateAtomic(List<T> models);

    List<T> batchUpdateAtomic(List<T> models);

    void batchDeleteAtomic(List<ID> ids);

    List<BatchResultWithData<T>> batchCreate(List<T> models, int batchSize);

    List<BatchResultWithData<T>> batchUpdate(List<T> models, int batchSize);

    List<BatchResultWithData<ID>> batchDelete(List<ID> ids, int batchSize);

    List<T> batchCreateAtomic(List<T> models, int batchSize);

    List<T> batchUpdateAtomic(List<T> models, int batchSize);

    void batchDeleteAtomic(List<ID> ids, int batchSize);

}
