package pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Outbound repository port for durable persistence of audit log events.
 * Fully aligned with {Entidad}RepositoryPort naming convention.
 */
public interface AuditLogRepositoryPort {

    Mono<Void> recordAudit(String action, String orderCode, String resourceId,
                           String actor, String tenant, String result,
                           String correlationId, String errorDetails);
}
