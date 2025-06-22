package io.github.egorkor.webutils.exception;

public class SoftDeleteUnsupportedException extends RuntimeException {
    public SoftDeleteUnsupportedException(String message) {
        super(message);
    }
}
