package pe.edu.vallegrande.caneteseguro.fleet.infrastructure.exception.custom;

/**
 * Custom technical exception for client integration failures (STD-043 & STD-087).
 * Encapsulates infrastructure HTTP errors from external services without exposing internal details.
 */
public class ClientIntegrationException extends RuntimeException {

    private final int statusCode;

    public ClientIntegrationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ClientIntegrationException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
