package io.github.egorkor.webutils.event.crud;

public class EntitySoftDeletedEvent<T> extends EntityActionEvent<T> {
    public EntitySoftDeletedEvent(Object source, T entity) {
        super(source, entity);
    }
}
