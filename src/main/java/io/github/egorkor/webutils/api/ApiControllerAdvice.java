package io.github.egorkor.webutils.api;

import io.github.egorkor.webutils.dto.EntityProcessingErrorDto;
import io.github.egorkor.webutils.dto.GenericErrorDto;
import io.github.egorkor.webutils.exception.EntityProcessingException;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.exception.ValidationException;
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
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@RestControllerAdvice
public class ApiControllerAdvice {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public GenericErrorDto<Void> handleException(Exception e) {
        return GenericErrorDto.<Void>builder()
                .code(500)
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler
    public GenericErrorDto<Map<String, List<String>>> handleValidationException(ValidationException e) {
        return GenericErrorDto.<Map<String, List<String>>>builder()
                .error(e.getErrors())
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler
    public GenericErrorDto<Void> handleNotFoundException(ResourceNotFoundException e) {
        return GenericErrorDto.<Void>builder()
                .code(404)
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler
    public GenericErrorDto<EntityProcessingErrorDto> handleEntityProcessingException(EntityProcessingException e) {
        String detailedMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        return GenericErrorDto.<EntityProcessingErrorDto>builder()
                .code(422)
                .error(new EntityProcessingErrorDto(e.getEntityType().getName(), e.getOperation(), detailedMessage))
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }


}
