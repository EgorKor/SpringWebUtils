package io.github.egorkor.webutils.api;

import io.github.egorkor.webutils.dto.EntityProcessingErrorDto;
import io.github.egorkor.webutils.dto.GenericErrorDto;
import io.github.egorkor.webutils.exception.BatchOperationException;
import io.github.egorkor.webutils.exception.EntityProcessingException;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Глобальный обработчик исключений
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Slf4j
@RestControllerAdvice
public class GenericApiControllerAdvice {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public GenericErrorDto<Void> handleException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return GenericErrorDto.<Void>builder()
                .code(500)
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler
    public GenericErrorDto<Map<String, List<String>>> handleValidationException(ValidationException e) {
        log.error("Validation error: {}", e.getMessage(), e);
        return GenericErrorDto.<Map<String, List<String>>>builder()
                .error(e.getErrors())
                .message(e.getMessage())
                .code(400)
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler
    public GenericErrorDto<Void> handleNotFoundException(ResourceNotFoundException e) {
        log.error("Resource not found: {}", e.getMessage(), e);
        return GenericErrorDto.<Void>builder()
                .code(404)
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler
    public GenericErrorDto<Void> handleBatchOperationException(BatchOperationException e) {
        log.error("Batch operation error: {}", e.getMessage(), e);
        return GenericErrorDto.<Void>builder()
                .message(e.getMessage())
                .code(422)
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler
    public GenericErrorDto<EntityProcessingErrorDto> handleEntityProcessingException(EntityProcessingException e) {
        String detailedMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        log.error("Entity processing error: {}", detailedMessage);
        return GenericErrorDto.<EntityProcessingErrorDto>builder()
                .code(422)
                .error(new EntityProcessingErrorDto(e.getEntityType().getName(), e.getOperation(), detailedMessage))
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }


}
