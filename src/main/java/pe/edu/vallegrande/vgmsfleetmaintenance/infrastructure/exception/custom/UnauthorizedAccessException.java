package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.exception.custom;

/**
 * Custom exception thrown when access to a resource is unauthorized or forbidden.
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
