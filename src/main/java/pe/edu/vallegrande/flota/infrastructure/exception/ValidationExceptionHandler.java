package pe.edu.vallegrande.flota.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import pe.edu.vallegrande.flota.infrastructure.dto.common.ResponseDto;
import pe.edu.vallegrande.flota.infrastructure.dto.common.ValidationError;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Manejador de errores de validación de Bean Validation (@Valid) según PR241.
 */
@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ResponseDto<List<ValidationError>>>> handleValidation(WebExchangeBindException ex) {
        List<ValidationError> errors = ex.getFieldErrors().stream()
                .map(fieldError -> ValidationError.of(
                        fieldError.getField(),
                        fieldError.getRejectedValue(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        ResponseDto<List<ValidationError>> response = new ResponseDto<>(
                false,
                errors,
                "Errores de validación en la solicitud",
                java.time.Instant.now()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }
}
