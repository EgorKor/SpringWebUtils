package io.github.egorkor.webutils.exception;

import lombok.Getter;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Getter
public class EntityProcessingException extends RuntimeException {
    private final Class<?> entityType;
    private final EntityOperation operation;

    public EntityProcessingException(String message, Throwable cause, Class<?> entityType, EntityOperation operation) {
        super(message, cause);
        this.entityType = entityType;
        this.operation = operation;
    }
}
