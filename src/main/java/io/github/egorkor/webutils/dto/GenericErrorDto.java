package io.github.egorkor.webutils.dto;

import lombok.Builder;
import lombok.Data;


/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Builder
@Data
public class GenericErrorDto<T> {
    private T error;
    private String message;
    private Integer code;
    private String date;
}
