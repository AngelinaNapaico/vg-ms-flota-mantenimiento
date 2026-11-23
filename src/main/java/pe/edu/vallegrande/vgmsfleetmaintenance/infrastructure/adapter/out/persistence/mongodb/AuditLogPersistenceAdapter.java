package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.AuditLogRepositoryPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.AuditLogDocument;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.AuditLogRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Persistence adapter providing durable storage of audit events in MongoDB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogPersistenceAdapter implements AuditLogRepositoryPort {

    private final AuditLogRepository repository;

    @Override
    public Mono<Void> recordAudit(String action, String orderCode, String resourceId,
            String actor, String tenant, String result,
            String correlationId, String errorDetails) {
        AuditLogDocument doc = AuditLogDocument.builder()
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

        var saveMono = repository.save(doc);
        if (saveMono == null) {
            return Mono.empty();
        }
        return saveMono
                .doOnError(err -> log.warn("Failed to persist durable audit log: {}", err.getClass().getSimpleName()))
                .then();
    }
}
