package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.vgmsfleetmaintenance.application.mapper.FieldNameMapper;
import pe.edu.vallegrande.vgmsfleetmaintenance.application.mapper.ValidationMessageMapper;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized Global Exception Handler.
 * Fully compliant with STD-048, STD-073, STD-074, STD-077:
 * - Sanitizes all client-facing responses avoiding internal structure leakage.
 * - Retains full technical causes and stack traces in internal diagnostic logs.
 * - Standard response payload: timestamp, status, error, message, and optional errors.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleDomain(DomainException ex) {
        log.warn("Domain exception: {}", ex.getMessage(), ex);
        return Mono.just(buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Business rule violation or invalid state"));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidation(WebExchangeBindException ex) {
        log.warn("Validation error: {}", ex.getMessage(), ex);
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                String publicField = FieldNameMapper.getPublicFieldName(fieldError.getField());
                String publicMessage = ValidationMessageMapper.mapValidationMessage(fieldError.getDefaultMessage());
                fieldErrors.put(publicField, publicMessage);
            }
        });

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("message", "Validation constraints failed");
        body.put("errors", fieldErrors);

        return Mono.just(ResponseEntity.badRequest().body(body));
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failure: {}", ex.getMessage(), ex);
        return Mono.just(buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Authentication required or invalid credentials"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage(), ex);
        return Mono.just(buildResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "Access denied: insufficient permissions"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data integrity conflict: {}", ex.getMessage(), ex);
        return Mono.just(buildResponse(
                HttpStatus.CONFLICT,
                "Conflict",
                "Data integrity conflict in persistent storage"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleResponseStatus(ResponseStatusException ex) {
        log.warn("Response status exception: {}", ex.getMessage(), ex);
        return Mono.just(buildResponse(
                HttpStatus.resolve(ex.getStatusCode().value()),
                "Resource Error",
                "Request cannot be processed by the server"));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGeneric(Exception ex) {
        log.error("Unhandled error: {}", ex.getMessage(), ex);
        return Mono.just(buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred"));
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        HttpStatus finalStatus = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", finalStatus.value());
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(finalStatus).body(body);
    }
}
