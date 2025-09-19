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


    public ValidationException(String message, Map<String, List<String>> errors) {
        super(message);
        this.errors = errors;
        this.layer = Layer.CONTROLLER;
    }

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

    public <T> ValidationException(String message, Set<ConstraintViolation<T>> violations) {
        super(message);
        this.layer = Layer.SERVICE;
        errors = new HashMap<>();
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

    public enum Layer {
        CONTROLLER, SERVICE
    }
}
