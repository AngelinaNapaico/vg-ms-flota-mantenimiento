package vg_ms.flota.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
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
}
