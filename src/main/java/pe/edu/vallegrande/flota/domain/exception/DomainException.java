package pe.edu.vallegrande.flota.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio para violaciones de reglas de negocio e invariantes.
 */
public class DomainException extends RuntimeException {

    private final HttpStatus status;

    public DomainException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public DomainException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
