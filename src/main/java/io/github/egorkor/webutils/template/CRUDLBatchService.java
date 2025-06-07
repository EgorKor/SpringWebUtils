package io.github.egorkor.webutils.template;

import java.util.List;

public interface CRUDLBatchService<T, ID> extends CRUDLService<T, ID> {
    List<BatchResultWithData<T>> batchCreate(List<T> models);

    List<BatchResult> batchUpdate(List<T> models);

    List<BatchResult> batchDelete(List<ID> ids);
}
