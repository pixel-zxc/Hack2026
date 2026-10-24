package com.jsmc.spacemap.project.web;

import com.jsmc.spacemap.project.service.exception.ConflictException;
import com.jsmc.spacemap.project.service.exception.ForbiddenOperationException;
import com.jsmc.spacemap.project.service.exception.PayloadTooLargeException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;
import com.jsmc.spacemap.project.service.exception.UnsupportedMediaTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenOperationException exception) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException exception) {
        return response(HttpStatus.UNAUTHORIZED, "Неверный email или пароль");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Некорректный запрос";
        }
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({PayloadTooLargeException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<Map<String, Object>> handlePayloadTooLarge(Exception exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : "Файл слишком большой";
        return response(HttpStatus.PAYLOAD_TOO_LARGE, message);
    }

    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMedia(UnsupportedMediaTypeException exception) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        ));
    }
}
