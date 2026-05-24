package com.hse.adminservice.common.error;

import com.hse.adminservice.common.time.TimeProvider;
import com.hse.adminservice.rbac.authorization.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final TimeProvider timeProvider;

    public GlobalExceptionHandler(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        log.error("Resource not found exception handled", ex);
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        log.error("Conflict exception handled", ex);
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Database constraint violation handled", ex);
        return build(HttpStatus.CONFLICT, "Операция конфликтует с существующими данными или текущим состоянием.");
    }


    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(Exception ex) {
        log.error("Multipart upload size exception handled", ex);
        return build(HttpStatus.BAD_REQUEST, "Размер файла не должен превышать 75 МБ.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        log.error("Validation exception handled", ex);
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(e -> e.getField() + ": " + translateValidationMessage(e.getDefaultMessage()))
                .orElse("Ошибка валидации");

        return build(HttpStatus.BAD_REQUEST, message);
    }

    private String translateValidationMessage(String message) {
        if (message == null) {
            return "Поле заполнено некорректно";
        }
        return switch (message) {
            case "must not be blank" -> "Заполните поле";
            case "must not be null" -> "Укажите значение";
            case "must be a well-formed email address" -> "Укажите корректный email";
            case "must be greater than or equal to 0" -> "Значение не может быть отрицательным";
            case "must be greater than 0" -> "Значение должно быть больше нуля";
            default -> message;
        };
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        log.error("Bad credentials exception handled", ex);
        return build(HttpStatus.UNAUTHORIZED, "Неверные учетные данные");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        log.error("Access denied exception handled", ex);
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        log.error("Unexpected exception handled", ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() == null ? "Произошла непредвиденная ошибка" : ex.getMessage()
        );
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.builder()
                .status(status.value())
                .message(message)
                .timestamp(timeProvider.now())
                .build());
    }
}
