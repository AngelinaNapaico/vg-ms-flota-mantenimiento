package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.exception.custom;

/**
 * Technical exception thrown when an external upstream dependency fails or is unavailable.
 */
public class UpstreamServiceException extends RuntimeException {

    public UpstreamServiceException(String message) {
        super(message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
