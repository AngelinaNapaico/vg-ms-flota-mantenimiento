package pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception;

/**
 * Domain exception for business rule violations, invariants, and missing domain entities.
 * Free of Spring or HTTP framework dependencies.
 */
public class DomainException extends RuntimeException {

    private final boolean notFound;

    public DomainException(String message) {
        this(message, false);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
        this.notFound = false;
    }

    private DomainException(String message, boolean notFound) {
        super(message);
        this.notFound = notFound;
    }

    public static DomainException notFound(String message) {
        return new DomainException(message, true);
    }

    public boolean isNotFound() {
        return notFound;
    }
}
