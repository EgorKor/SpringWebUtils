package io.github.egorkor.webutils.event.crud;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public class EntitySoftDeletedEvent<T> extends EntityActionEvent<T> {
    public EntitySoftDeletedEvent(Object source, T entity) {
        super(source, entity);
    }
}
