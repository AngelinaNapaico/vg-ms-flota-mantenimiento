package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.exception.custom;

/**
 * Technical exception thrown when an external upstream dependency fails or is unavailable.
 * Carries sanitized operational metadata without propagating raw HTTP client exceptions.
 */
public class UpstreamServiceException extends RuntimeException {

    private final String operation;
    private final Integer upstreamStatus;
    private final String technicalCause;

    public UpstreamServiceException(String message) {
        this(message, "UNKNOWN", null, "UNKNOWN");
    }

    public UpstreamServiceException(
            String message,
            String operation,
            Integer upstreamStatus,
            String technicalCause) {
        super(message);
        this.operation = operation != null && !operation.isBlank() ? operation : "UNKNOWN";
        this.upstreamStatus = upstreamStatus;
        this.technicalCause = technicalCause != null && !technicalCause.isBlank() ? technicalCause : "UNKNOWN";
    }

    public String getOperation() {
        return operation;
    }

    public Integer getUpstreamStatus() {
        return upstreamStatus;
    }

    public String getTechnicalCause() {
        return technicalCause;
    }
}
