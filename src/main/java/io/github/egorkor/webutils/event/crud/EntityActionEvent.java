package io.github.egorkor.webutils.event.crud;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EntityActionEvent<T> extends ApplicationEvent {
    private final T entity;

    public EntityActionEvent(Object source, T entity) {
        super(source);
        this.entity = entity;
    }
}
