package be.ahm282.QuickClock.infrastructure.config;

import be.ahm282.QuickClock.domain.exception.*;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Common error body:
     * {
     *   "timestamp": "...",
     *   "status": 400,
     *   "error": "Bad Request",
     *   "message": "...",
     *   "type": "ValidationException"
     *   // + optional fields like "validationErrors"
     * }
     */
    private Map<String, Object> buildBody(HttpStatus status, String message, String type) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("type", type);
        return body;
    }

    // ---------------------------
    // Domain-level exceptions
    // ---------------------------

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomainException(DomainException ex) {
        HttpStatus status = ex.getHttpStatus();

        log.warn("DomainException [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage());

        Map<String, Object> body = buildBody(status, ex.getMessage(), ex.getClass().getSimpleName());
        return ResponseEntity.status(status).body(body);
    }

    // ---------------------------
    // Validation / Bean Validation
    // ---------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        Map<String, List<String>> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> {
                    errors.computeIfAbsent(e.getField(), k -> new ArrayList<>())
                            .add(e.getDefaultMessage());
                });

        Map<String, Object> body = buildBody(status, "Validation failed", ex.getClass().getSimpleName());
        body.put("validationErrors", errors);

        log.debug("MethodArgumentNotValidException: {}", errors);

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        Map<String, Object> body = buildBody(status, "Constraint violation", ex.getClass().getSimpleName());
        body.put("details", ex.getMessage());

        log.debug("ConstraintViolationException: {}", ex.getMessage());

        return ResponseEntity.status(status).body(body);
    }

    // ---------------------------
    // Auth / Security-related
    // ---------------------------

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> handleJwtException(JwtException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        Map<String, Object> body = buildBody(status, "Invalid or expired token", ex.getClass().getSimpleName());
        log.warn("JwtException: {}", ex.getMessage());

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;

        Map<String, Object> body = buildBody(status, "Access denied", ex.getClass().getSimpleName());
        log.warn("AccessDeniedException: {}", ex.getMessage());

        return ResponseEntity.status(status).body(body);
    }

    // ---------------------------
    // Fallback
    // ---------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> fallback(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("Unhandled exception", ex);

        // Do NOT expose internal type/message to the client here.
        Map<String, Object> body = buildBody(status, "Internal Server Error", "InternalServerError");
        return ResponseEntity.status(status).body(body);
    }
}
