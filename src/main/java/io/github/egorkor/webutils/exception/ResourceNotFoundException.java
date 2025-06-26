package io.github.egorkor.webutils.exception;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
