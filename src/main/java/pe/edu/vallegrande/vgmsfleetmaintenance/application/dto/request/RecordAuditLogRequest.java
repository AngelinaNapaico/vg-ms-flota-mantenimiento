package pe.edu.vallegrande.vgmsfleetmaintenance.application.dto.request;

/**
 * Immutable application request encapsulating audit log details
 * produced during maintenance order lifecycle operations.
 */
public record RecordAuditLogRequest(
        String action,
        String orderCode,
        String resourceId,
        String actor,
        String tenant,
        String result,
        String correlationId,
        String errorType) {
}
