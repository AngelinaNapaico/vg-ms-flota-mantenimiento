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
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import pe.edu.vallegrande.vgmsfleetmaintenance.application.mapper.FieldNameMapper;
import pe.edu.vallegrande.vgmsfleetmaintenance.application.mapper.ValidationMessageMapper;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.exception.custom.UpstreamServiceException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized global exception handler for REST controllers.
 * Consolidates HTTP status resolution by exception type and ensures secure
 * log sanitization without exposing internal structure or traces to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(DomainException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleDomain(DomainException ex) {
        log.warn("Domain exception - type: {}, notFound: {}", ex.getClass().getSimpleName(), ex.isNotFound());

        if (ex.isNotFound()) {
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

    @ExceptionHandler(UpstreamServiceException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleUpstream(
            UpstreamServiceException ex, ServerWebExchange exchange) {
        String errorRef = UUID.randomUUID().toString().substring(0, 8);
        String statusText = ex.getUpstreamStatus() != null ? String.valueOf(ex.getUpstreamStatus()) : "TRANSPORT";
        log.warn(
                "Upstream integration error [ref={}, method={}, path={}, type={}, operation={}, upstreamStatus={}, technicalCause={}]",
                errorRef, resolveMethod(exchange), resolveSanitizedPath(exchange),
                ex.getClass().getSimpleName(), ex.getOperation(), statusText, ex.getTechnicalCause());
        return Mono.just(buildResponse(
                HttpStatus.BAD_GATEWAY,
                "Bad Gateway",
                "Upstream external service communication failed"));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleException(
            Exception ex, ServerWebExchange exchange) {
        String errorRef = UUID.randomUUID().toString().substring(0, 8);
        String method = resolveMethod(exchange);
        String path = resolveSanitizedPath(exchange);
        String type = ex.getClass().getSimpleName();

        if (ex instanceof AccessDeniedException) {
            log.warn("Access denied [ref={}, method={}, path={}, type={}]", errorRef, method, path, type);
            return Mono.just(buildResponse(
                    HttpStatus.FORBIDDEN,
                    "Forbidden",
                    "Access denied: insufficient permissions"));
        }

        if (ex instanceof AuthenticationException) {
            log.warn("Authentication failure [ref={}, method={}, path={}, type={}]", errorRef, method, path, type);
            return Mono.just(buildResponse(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "Authentication required or invalid credentials"));
        }

        if (ex instanceof ServerWebInputException) {
            log.warn("Malformed input [ref={}, method={}, path={}, type={}]", errorRef, method, path, type);
            return Mono.just(buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    "Malformed request payload or invalid parameter format"));
        }

        if (ex instanceof ResponseStatusException rse) {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
            HttpStatus status = resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
            log.warn("Response status error [ref={}, method={}, path={}, status={}, type={}]",
                    errorRef, method, path, status.value(), type);
            return Mono.just(buildResponse(
                    status,
                    status.getReasonPhrase(),
                    resolvePublicStatusMessage(status)));
        }

        if (ex instanceof DataIntegrityViolationException) {
            log.warn("Data integrity conflict [ref={}, method={}, path={}, type={}]", errorRef, method, path, type);
            return Mono.just(buildResponse(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "Data integrity conflict in persistent storage"));
        }

        log.error("Unhandled server error [ref={}, method={}, path={}, type={}, cause={}]",
                errorRef, method, path, type, resolveCauseType(ex));
        return Mono.just(buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred"));
    }

    private String resolveMethod(ServerWebExchange exchange) {
        if (exchange == null || exchange.getRequest().getMethod() == null) {
            return "UNKNOWN";
        }
        return exchange.getRequest().getMethod().name();
    }

    private String resolveSanitizedPath(ServerWebExchange exchange) {
        if (exchange == null) {
            return "UNKNOWN";
        }
        String rawPath = exchange.getRequest().getPath().value();
        String sanitized = rawPath.replaceAll("[\\r\\n\\t]", "_");
        return sanitized.length() > 120 ? sanitized.substring(0, 120) : sanitized;
    }

    private String resolveCauseType(Throwable ex) {
        if (ex == null || ex.getCause() == null) {
            return "DIRECT";
        }
        Throwable cursor = ex.getCause();
        int depth = 0;
        while (cursor.getCause() != null && cursor.getCause() != cursor && depth < 5) {
            cursor = cursor.getCause();
            depth++;
        }
        return cursor.getClass().getSimpleName();
    }

    private String resolvePublicStatusMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Malformed request payload or invalid parameter format";
            case UNAUTHORIZED -> "Authentication required or invalid credentials";
            case FORBIDDEN -> "Access denied: insufficient permissions";
            case NOT_FOUND -> "Requested resource was not found";
            case METHOD_NOT_ALLOWED -> "HTTP method is not supported for this endpoint";
            case CONFLICT -> "Request conflicts with the current state of the resource";
            case UNSUPPORTED_MEDIA_TYPE -> "Unsupported media type in request";
            default -> "Request cannot be processed by the server";
        };
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
