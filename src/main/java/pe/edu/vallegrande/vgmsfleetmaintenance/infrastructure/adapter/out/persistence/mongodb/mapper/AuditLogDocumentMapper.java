package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.mapper;

import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.AuditLogDocument;

import java.time.Instant;

/**
 * Mapper for constructing durable audit documents.
 */
public final class AuditLogDocumentMapper {

    private AuditLogDocumentMapper() {}

    public static AuditLogDocument toDocument(String action, String orderCode, String resourceId,
                                             String actor, String tenant, String result,
                                             String correlationId, String errorDetails) {
        return AuditLogDocument.builder()
                .timestamp(Instant.now())
                .action(action)
                .orderCode(orderCode)
                .resourceId(resourceId)
                .actor(actor)
                .tenant(tenant)
                .result(result)
                .correlationId(correlationId)
                .errorDetails(errorDetails)
                .build();
    }
}
