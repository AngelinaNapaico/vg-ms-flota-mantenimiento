package pe.edu.vallegrande.vgmsfleetmaintenance.application.dto.response;

import java.time.Instant;

/**
 * Immutable application response representing a normalized audit log entry
 * ready for structured diagnostic logging and durable persistence.
 */
public record AuditLogResponse(
        Instant timestamp,
        String action,
        String orderCode,
        String resourceId,
        String actor,
        String tenant,
        String result,
        String correlationId,
        String errorType) {
}
