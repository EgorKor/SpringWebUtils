package io.github.egorkor.webutils.exception;

import lombok.Getter;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {
    private final Map<String, List<String>> errors;

    public ValidationException(BindingResult errors) {
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
}
