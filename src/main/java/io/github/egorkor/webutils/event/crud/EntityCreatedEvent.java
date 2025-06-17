package io.github.egorkor.webutils.event.crud;

public class EntityCreatedEvent<T> extends EntityActionEvent<T> {
    public EntityCreatedEvent(Object source, T entity) {
        super(source, entity);
    }
}
