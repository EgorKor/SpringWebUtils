package io.github.egorkor.webutils.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchResultWithDataImpl<T> implements BatchResultWithData<T> {
    private String message;
    private T data;
    private String details;
    private BatchOperationStatus status;

    @Override
    public T getData() {
        return data;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getDetails() {
        return details;
    }

    @Override
    public BatchOperationStatus getStatus() {
        return status;
    }


}
