package be.ahm282.QuickClock.infrastructure.config;

import be.ahm282.QuickClock.domain.exception.*;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -------------------------------------------------------------------------
    // 1. Standard Spring MVC Exceptions (JSON parse errors, 405, 400, etc.)
    // -------------------------------------------------------------------------
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        // This ensures even internal Spring errors (like 405 Method Not Allowed)
        // follow your custom ProblemDetail format.
        if (body instanceof ProblemDetail problemDetail) {
            enrichProblemDetail(problemDetail, (Exception) request.getAttribute("jakarta.servlet.error.exception", 0));
            return new ResponseEntity<>(problemDetail, headers, statusCode);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Exception");

        // Collect errors into a simple Map<Field, Message> or List<String>
        Map<String, List<String>> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        org.springframework.validation.FieldError::getField,
                        Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                ));

        problem.setProperty("validationErrors", validationErrors);
        enrichProblemDetail(problem, ex);

        return ResponseEntity.badRequest().body(problem);
    }

    // -------------------------------------------------------------------------
    // 2. Domain Exceptions
    // -------------------------------------------------------------------------
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex) {
        // DomainException knows its own HTTP status
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
        problem.setTitle(ex.getClass().getSimpleName());

        // Special case: TokenException has an extra userId field
        // We can use reflection or explicit checks if needed,
        // or just add it via setProperty if specific exceptions expose data.

        enrichProblemDetail(problem, ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(problem);
    }

    // -------------------------------------------------------------------------
    // 3. Validation (JPA/Hibernate Validator outside of Controllers)
    // -------------------------------------------------------------------------
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Constraint Violation");
        enrichProblemDetail(problem, ex);
        return ResponseEntity.badRequest().body(problem);
    }

    // -------------------------------------------------------------------------
    // 4. Security Exceptions
    // -------------------------------------------------------------------------
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class, JwtException.class})
    public ResponseEntity<ProblemDetail> handleSecurityException(Exception ex) {
        HttpStatus status = (ex instanceof AccessDeniedException) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        String message = (ex instanceof AccessDeniedException) ? "Access Denied" : "Authentication Failed";

        // Don't leak internal security details to a client, use a generic message or safe ex.getMessage()
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(ex.getClass().getSimpleName());
        problem.setDetail(ex.getMessage());

        enrichProblemDetail(problem, ex);
        return ResponseEntity.status(status).body(problem);
    }

    // -------------------------------------------------------------------------
    // 5. SSE & Async (Silent)
    // -------------------------------------------------------------------------
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public void handleAsyncDisconnect() {
        // Client disconnected, do nothing.
    }

    // -------------------------------------------------------------------------
    // 6. Fallback
    // -------------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnknownException(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled exception [ID: {}]", errorId, ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal Server Error");
        problem.setProperty("errorId", errorId);

        enrichProblemDetail(problem, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // -------------------------------------------------------------------------
    // Helper: Enrich ProblemDetail with standard fields (Timestamp, etc)
    // -------------------------------------------------------------------------
    private void enrichProblemDetail(ProblemDetail problem, Exception ex) {
        problem.setProperty("timestamp", Instant.now());
        // 'type' is a standard RFC 7807 field usually pointing to documentation.
        // Default it to "about:blank".
        problem.setType(URI.create("urn:problem-type:" + (ex != null ? ex.getClass().getSimpleName() : "Unknown")));
    }
}
