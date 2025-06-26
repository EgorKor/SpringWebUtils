package io.github.egorkor.webutils.event.crud;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Getter
public class EntityDeletingEvent<ID> extends ApplicationEvent {
    private final ID id;

    public EntityDeletingEvent(Object source, ID id) {
        super(source);
        this.id = id;
    }
}
