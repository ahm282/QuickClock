package be.ahm282.QuickClock.infrastructure.config;

import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import be.ahm282.QuickClock.domain.exception.DomainException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private HashMap<String, String> FormatExceptionBody(String message, String simpleName, Exception ex) {
        HashMap<String, String> body = new HashMap<>();
        body.put("timestamp", java.time.Instant.now().toString());
        body.put("message", message);
        body.put("type", simpleName);
        body.put("status", "400");

        return body;
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, String>> handleDomainException(DomainException ex) {
        HashMap<String, String> body = FormatExceptionBody(ex.getMessage(), ex.getClass().getSimpleName(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRule(BusinessRuleException ex) {
        HashMap<String, String> body = FormatExceptionBody(ex.getMessage(), ex.getClass().getSimpleName(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleMethodArg(MethodArgumentNotValidException ex) {
        Map<String,String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("validationErrors", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String,Object>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> fallback(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal Server Error"));
    }
}
