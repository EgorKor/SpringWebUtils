package io.github.egorkor.webutils.event.crud;

public class EntityUpdatingEvent<T> extends EntityActionEvent<T> {
    public EntityUpdatingEvent(Object source, T entity) {
        super(source, entity);
    }
}
