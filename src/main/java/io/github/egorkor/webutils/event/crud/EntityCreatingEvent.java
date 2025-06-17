package io.github.egorkor.webutils.event.crud;

public class EntityCreatingEvent<T> extends EntityActionEvent<T> {
    public EntityCreatingEvent(Object source, T entity) {
        super(source, entity);
    }
}
