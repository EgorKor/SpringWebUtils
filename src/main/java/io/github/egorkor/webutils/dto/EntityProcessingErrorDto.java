package io.github.egorkor.webutils.dto;

import io.github.egorkor.webutils.exception.EntityOperation;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public record EntityProcessingErrorDto(String entity,
                                       EntityOperation operation,
                                       String detailedMessage) {
}
