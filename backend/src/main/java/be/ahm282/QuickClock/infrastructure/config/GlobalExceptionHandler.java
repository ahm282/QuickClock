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
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -------------------------------------------------------------------------
    // 1. Domain Exceptions (The Central Mapping Hub)
    // -------------------------------------------------------------------------
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex) {
        HttpStatus status = switch (ex) {
            case AuthenticationException e -> HttpStatus.UNAUTHORIZED;
            case TokenException e -> HttpStatus.UNAUTHORIZED;
            case NotFoundException e -> HttpStatus.NOT_FOUND;
            case BusinessRuleException e -> HttpStatus.CONFLICT;
            case UsernameAlreadyExistsException e -> HttpStatus.CONFLICT;
            case RateLimitException e -> HttpStatus.TOO_MANY_REQUESTS;
            case ValidationException e -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST; // Default fallback for generic DomainException
        };

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(splitCamelCase(ex.getClass().getSimpleName()));

        // Special handling for TokenException properties
        if (ex instanceof TokenException tokenEx && tokenEx.getUserId() != null) {
            problem.setProperty("userId", tokenEx.getUserId());
        }

        enrichProblemDetail(problem, ex);
        return ResponseEntity.status(status).body(problem);
    }

    // -------------------------------------------------------------------------
    // 2. Standard Spring MVC Exceptions (Validation, etc.)
    // -------------------------------------------------------------------------
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {
            enrichProblemDetail(problemDetail, (Exception) request.getAttribute("jakarta.servlet.error.exception", 0));
            return new ResponseEntity<>(problemDetail, headers, statusCode);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");

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
    // 3. Low-level Validation (Hibernate/JPA)
    // -------------------------------------------------------------------------
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Constraint Violation");
        enrichProblemDetail(problem, ex);
        return ResponseEntity.badRequest().body(problem);
    }

    // -------------------------------------------------------------------------
    // 4. Security Framework Exceptions (Spring Security / JWT Library)
    // -------------------------------------------------------------------------
    // This handles the FRAMEWORK exceptions (org.springframework.security...)
    @ExceptionHandler({AccessDeniedException.class, JwtException.class})
    public ResponseEntity<ProblemDetail> handleFrameworkSecurityException(Exception ex) {
        HttpStatus status = (ex instanceof AccessDeniedException) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        String message = (ex instanceof AccessDeniedException) ? "Access Denied" : "Invalid Token";

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(splitCamelCase(ex.getClass().getSimpleName()));

        enrichProblemDetail(problem, ex);
        return ResponseEntity.status(status).body(problem);
    }

    // -------------------------------------------------------------------------
    // 5. Fallback (Catch All)
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
    // Helpers
    // -------------------------------------------------------------------------
    private void enrichProblemDetail(ProblemDetail problem, Exception ex) {
        problem.setProperty("timestamp", Instant.now());
        problem.setType(URI.create("urn:problem-type:" + (ex != null ? ex.getClass().getSimpleName() : "Unknown")));
    }

    // Turns "BusinessRuleException" into "Business Rule Exception" for better UI display
    private String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }
}