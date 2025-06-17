package io.github.egorkor.webutils.event.batching;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class BatchCreatedAtomicEvent<T> extends ApplicationEvent {
    private final List<T> batch;

    public BatchCreatedAtomicEvent(Object source, List<T> batch) {
        super(source);
        this.batch = batch;
    }
}
