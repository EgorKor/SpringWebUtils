package io.github.egorkor.webutils.event.batching;

import io.github.egorkor.webutils.service.batching.BatchResultWithData;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Getter
public class BatchDeletedEvent<T, ID> extends ApplicationEvent {
    private final List<BatchResultWithData<ID>> results;
    private final Class<T> entityType;


    public BatchDeletedEvent(Object source, List<BatchResultWithData<ID>> results, Class<T> entityType) {
        super(source);
        this.results = results;
        this.entityType = entityType;
    }
}
