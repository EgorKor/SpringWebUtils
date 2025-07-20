package io.github.egorkor.webutils.exception;

import jakarta.validation.ConstraintViolation;
import lombok.Getter;
import org.springframework.validation.BindingResult;

import java.util.*;


/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Getter
public class ValidationException extends RuntimeException {
    private final Map<String, List<String>> errors;
    private final Layer layer;

    public ValidationException(String message, BindingResult errors) {
        super(message);
        this.layer = Layer.CONTROLLER;
        this.errors = new HashMap<>();
        errors.getFieldErrors().forEach(e -> {
            if (this.errors.containsKey(e.getField())) {
                this.errors.get(e.getField()).add(e.getDefaultMessage());
            } else {
                List<String> list = new ArrayList<>();
                list.add(e.getDefaultMessage());
                this.errors.put(e.getField(), list);
            }
        });
    }

    public <T> ValidationException(Set<ConstraintViolation<T>> violations) {
        errors = new HashMap<>();
        this.layer = Layer.SERVICE;
        for (ConstraintViolation<T> violation : violations) {
            String field = violation.getPropertyPath().toString();
            if (this.errors.containsKey(field)) {
                this.errors.get(field).add(violation.getMessage());
            } else {
                List<String> list = new ArrayList<>();
                list.add(violation.getMessage());
                this.errors.put(field, list);
            }
        }
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        String errorDetails = "Validation Errors: " + this.errors;
        if (message != null && !message.isEmpty()) {
            return message + ": " + errorDetails;
        }
        return errorDetails;
    }

    public enum Layer {
        CONTROLLER, SERVICE
    }
}
