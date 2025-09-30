package io.github.egorkor.webutils.api;

import io.github.egorkor.webutils.dto.EntityProcessingErrorDto;
import io.github.egorkor.webutils.dto.GenericErrorDto;
import io.github.egorkor.webutils.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
//import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
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
@RestControllerAdvice
@Slf4j
public class GenericApiControllerAdvice {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public GenericErrorDto<Void> handleException(Exception e) {
        log.error("Неожиданная ошибка: {}", e.getMessage(), e);
        return GenericErrorDto.<Void>builder()
                .code(500)
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler
    public GenericErrorDto<Void> handleResourceUniqueException(ResourceUniqueException e) {
        log.warn("Ресурс не уникален : {}", e.getMessage(), e);
        return GenericErrorDto.<Void>builder()
                .code(400)
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler
    public GenericErrorDto<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("Ошибка аутентификации: {}", e.getMessage());
        return GenericErrorDto.<Void>builder()
                .code(401)
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public GenericErrorDto<Void> handleTemplateProcessingException(TemplateProcessingException e) {
        log.error("Ошибка обработки шаблона: {}", e.getMessage(), e);
        return GenericErrorDto.<Void>builder()
                .code(500)
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler
    public GenericErrorDto<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Ошибка отказа в доступе: {}", e.getMessage());
        return GenericErrorDto.<Void>builder()
                .code(403)
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler
    public GenericErrorDto<Void> handleInvalidParameterException(InvalidParameterException e) {
        log.warn("Ошибка недопустимого параметра: {}", e.getMessage(), e);
        return GenericErrorDto.<Void>builder()
                .code(400)
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler
    public GenericErrorDto<Map<String, List<String>>> handleValidationException(ValidationException e) {
        log.warn("Ошибка валидации: {}", e.getMessage(), e);
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
        log.warn("Ресурс не найден: {}", e.getMessage(), e);
        return GenericErrorDto.<Void>builder()
                .code(404)
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler
    public GenericErrorDto<Void> handleBatchOperationException(BatchOperationException e) {
        log.warn("Ошибка пакетной операции: {}", e.getMessage(), e);
        return GenericErrorDto.<Void>builder()
                .message(e.getMessage())
                .code(400)
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler
    public GenericErrorDto<EntityProcessingErrorDto> handleEntityProcessingException(EntityProcessingException e) {
        String detailedMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        log.warn("Ошибка обработки сущности: {}", detailedMessage, e);
        return GenericErrorDto.<EntityProcessingErrorDto>builder()
                .code(400)
                .error(new EntityProcessingErrorDto(e.getEntityType().getName(), e.getOperation(), detailedMessage))
                .message(e.getMessage())
                .date(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")).toString())
                .build();
    }


}
