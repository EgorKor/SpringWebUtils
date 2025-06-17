package io.github.egorkor.webutils.event.batching;

import io.github.egorkor.webutils.service.batching.BatchResultWithData;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class BatchCreatedEvent<T> extends ApplicationEvent {
    private final List<BatchResultWithData<T>> batch;

    public BatchCreatedEvent(Object source, List<BatchResultWithData<T>> batch) {
        super(source);
        this.batch = batch;
    }
}
