package io.github.egorkor.webutils.dto;

import io.github.egorkor.webutils.exception.EntityOperation;

public record EntityProcessingErrorDto(String entity, EntityOperation operation, String detailedMessage) {
}
