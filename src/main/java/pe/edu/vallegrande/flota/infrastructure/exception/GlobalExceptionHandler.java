package pe.edu.vallegrande.flota.infrastructure.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.edu.vallegrande.flota.domain.exception.DomainException;
import pe.edu.vallegrande.flota.infrastructure.dto.common.ErrorMessage;
import pe.edu.vallegrande.flota.infrastructure.dto.common.ResponseDto;
import reactor.core.publisher.Mono;

/**
 * Manejador global de excepciones según estándar PR241 - Sección 5.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public Mono<ResponseEntity<ResponseDto<ErrorMessage>>> handleDomainException(DomainException ex) {
        ErrorMessage error = ErrorMessage.of("BUSINESS_RULE_VIOLATION", ex.getMessage());
        return buildResponse(ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<ResponseDto<ErrorMessage>>> handleAuthentication(AuthenticationException ex) {
        ErrorMessage error = ErrorMessage.of("AUTHENTICATION_FAILED", ex.getMessage() != null ? ex.getMessage() : "No autenticado");
        return buildResponse(HttpStatus.UNAUTHORIZED, error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<ResponseDto<ErrorMessage>>> handleAccessDenied(AccessDeniedException ex) {
        ErrorMessage error = ErrorMessage.of("ACCESS_DENIED", ex.getMessage() != null ? ex.getMessage() : "Acceso denegado");
        return buildResponse(HttpStatus.FORBIDDEN, error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<ResponseDto<ErrorMessage>>> handleDataIntegrity(DataIntegrityViolationException ex) {
        ErrorMessage error = ErrorMessage.of("DUPLICATE_KEY_ERROR", "Conflicto de integridad de datos en MongoDB");
        return buildResponse(HttpStatus.CONFLICT, error);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ResponseDto<ErrorMessage>>> handleGeneral(Exception ex) {
        ErrorMessage error = ErrorMessage.of("INTERNAL_SERVER_ERROR", ex.getMessage() != null ? ex.getMessage() : "Error interno del servidor");
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, error);
    }

    private Mono<ResponseEntity<ResponseDto<ErrorMessage>>> buildResponse(HttpStatus status, ErrorMessage error) {
        return Mono.just(ResponseEntity.status(status).body(ResponseDto.error(error)));
    }
}
