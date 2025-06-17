package io.github.egorkor.webutils.event.crud;

public class EntityUpdatedEvent<T> extends EntityActionEvent<T>{
    public EntityUpdatedEvent(Object source, T entity) {
        super(source, entity);
    }
}
