package io.github.egorkor.webutils.event.crud;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public class EntityCreatingEvent<T> extends EntityActionEvent<T> {
    public EntityCreatingEvent(Object source, T entity) {
        super(source, entity);
    }
}
