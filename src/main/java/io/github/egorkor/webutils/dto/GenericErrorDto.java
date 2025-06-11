package io.github.egorkor.webutils.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GenericErrorDto<T> {
    private T error;
    private String message;
    private int code;
}
