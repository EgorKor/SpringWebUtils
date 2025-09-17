package io.github.egorkor.webutils.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T error;
    private String message;
    private Integer code;
    private String date;
}
