package pe.edu.vallegrande.vgmsfleetmaintenance.application.mapper;

import pe.edu.vallegrande.vgmsfleetmaintenance.application.dto.request.RecordAuditLogRequest;
import pe.edu.vallegrande.vgmsfleetmaintenance.application.dto.response.AuditLogResponse;

import java.time.Instant;
import java.util.UUID;

/**
 * Application mapper that normalizes and sanitizes audit event inputs
 * into structured application request and response records.
 */
public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static RecordAuditLogRequest toRequest(
            String action,
            String orderCode,
            String resourceId,
            String actor,
            String tenant,
            String result,
            String correlationId,
            Throwable error) {
        String sanitizedErrorType = error != null ? error.getClass().getSimpleName() : "NONE";
        return new RecordAuditLogRequest(
                action != null && !action.isBlank() ? action : "UNKNOWN",
                orderCode != null && !orderCode.isBlank() ? orderCode : "N/A",
                resourceId != null && !resourceId.isBlank() ? resourceId : "N/A",
                actor != null && !actor.isBlank() ? actor : "ANONYMOUS",
                tenant != null && !tenant.isBlank() ? tenant : "N/A",
                result != null && !result.isBlank() ? result : "UNKNOWN",
                correlationId != null && !correlationId.isBlank() ? correlationId : UUID.randomUUID().toString(),
                sanitizedErrorType);
    }

    public static AuditLogResponse toResponse(RecordAuditLogRequest request) {
        return new AuditLogResponse(
                Instant.now(),
                request.action(),
                request.orderCode(),
                request.resourceId(),
                request.actor(),
                request.tenant(),
                request.result(),
                request.correlationId(),
                request.errorType());
    }
}
