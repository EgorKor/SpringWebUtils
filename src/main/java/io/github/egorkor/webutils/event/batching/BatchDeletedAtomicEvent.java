package io.github.egorkor.webutils.event.batching;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class BatchDeletedAtomicEvent<T, ID> extends ApplicationEvent {
    private final List<ID> ids;
    private final Class<T> entityType;

    public BatchDeletedAtomicEvent(Object source, List<ID> ids, Class<T> entityType) {
        super(source);
        this.ids = ids;
        this.entityType = entityType;
    }
}
