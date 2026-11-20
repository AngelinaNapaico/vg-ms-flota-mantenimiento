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
import org.springframework.web.server.ServerWebInputException;
import pe.edu.vallegrande.vgmsfleetmaintenance.application.mapper.FieldNameMapper;
import pe.edu.vallegrande.vgmsfleetmaintenance.application.mapper.ValidationMessageMapper;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.exception.custom.UpstreamServiceException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized global exception handler for REST controllers.
 * Formats uniform error responses and ensures secure log sanitization
 * by recording diagnostic context without exposing internal traces to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleDomain(DomainException ex) {
        log.warn("Domain exception - type: {}", ex.getClass().getSimpleName());

        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")) {
            return Mono.just(buildResponse(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    "Requested maintenance order was not found"));
        }

        return Mono.just(buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Unprocessable Entity",
                "Business rule violation or unprocessable entity"));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidation(WebExchangeBindException ex) {
        log.warn("Validation error - type: {}, errorCount: {}",
                ex.getClass().getSimpleName(), ex.getErrorCount());
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
        body.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        body.put("error", "Unprocessable Entity");
        body.put("message", "Validation constraints failed");
        body.put("errors", fieldErrors);

        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleServerWebInput(ServerWebInputException ex) {
        log.warn("Malformed input - type: {}", ex.getClass().getSimpleName());
        return Mono.just(buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Malformed request payload or invalid parameter format"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failure - type: {}", ex.getClass().getSimpleName());
        return Mono.just(buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Authentication required or invalid credentials"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied - type: {}", ex.getClass().getSimpleName());
        return Mono.just(buildResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "Access denied: insufficient permissions"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity conflict - type: {}", ex.getClass().getSimpleName());
        return Mono.just(buildResponse(
                HttpStatus.CONFLICT,
                "Conflict",
                "Data integrity conflict in persistent storage"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleResponseStatus(ResponseStatusException ex) {
        log.warn("Response status error - status: {}, type: {}",
                ex.getStatusCode().value(), ex.getClass().getSimpleName());
        return Mono.just(buildResponse(
                HttpStatus.resolve(ex.getStatusCode().value()),
                "Resource Error",
                "Request cannot be processed by the server"));
    }

    @ExceptionHandler(UpstreamServiceException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleUpstream(UpstreamServiceException ex) {
        log.warn("External upstream service error during integration call - message: {}", ex.getMessage(), ex);
        return Mono.just(buildResponse(
                HttpStatus.BAD_GATEWAY,
                "Bad Gateway",
                "Upstream external service communication failed"));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGeneric(Exception ex) {
        log.error("Unhandled server error during operation execution - message: {}", ex.getMessage(), ex);
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
