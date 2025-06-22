package io.github.egorkor.webutils.exception;

import lombok.Getter;

@Getter
public class EntityProcessingException extends RuntimeException {
    private Class<?> entityType;
    private EntityOperation operation;

    public EntityProcessingException(String message, Throwable cause, Class<?> entityType, EntityOperation operation) {
        super(message, cause);
        this.entityType = entityType;
        this.operation = operation;
    }
}
