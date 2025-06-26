package io.github.egorkor.webutils.event.crud;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Getter
public class EntityDeletedEvent<T, ID> extends ApplicationEvent {
    private final ID id;
    private final Class<T> entityType;

    public EntityDeletedEvent(Object source, ID id, Class<T> entityType) {
        super(source);
        this.id = id;
        this.entityType = entityType;
    }
}
