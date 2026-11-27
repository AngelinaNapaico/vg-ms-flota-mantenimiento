package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.AuditLogRepositoryPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.AuditLogDocument;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.AuditLogRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Persistence adapter providing durable storage of audit events in MongoDB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogPersistenceAdapter implements AuditLogRepositoryPort {

    private final AuditLogRepository repository;

    @Override
    public Mono<Void> recordAudit(
            String action,
            String orderCode,
            String resourceId,
            String actor,
            String tenant,
            String result,
            String correlationId,
            String errorDetails) {
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

        return Mono.defer(() -> Objects.requireNonNull(
                        repository.save(doc),
                        "Audit repository returned null publisher"))
                .retry(1)
                .doOnError(err -> log.error("Failed to persist durable audit log [type={}]",
                        err.getClass().getSimpleName()))
                .then();
    }
}
