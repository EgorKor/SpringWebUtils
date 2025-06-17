package io.github.egorkor.webutils.event.crud;

public class EntitySoftDeletingEvent<T> extends EntityActionEvent<T>{
    public EntitySoftDeletingEvent(Object source, T entity) {
        super(source, entity);
    }
}
