package io.github.egorkor.webutils.exception;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public class SoftDeleteUnsupportedException extends RuntimeException {
    public SoftDeleteUnsupportedException(String message) {
        super(message);
    }
}
