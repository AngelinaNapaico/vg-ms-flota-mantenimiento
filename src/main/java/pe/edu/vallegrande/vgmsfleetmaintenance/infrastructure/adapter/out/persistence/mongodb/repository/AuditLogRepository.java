package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.AuditLogDocument;

/**
 * Spring Data Reactive repository for durable audit trail persistence.
 */
@Repository
public interface AuditLogRepository extends ReactiveMongoRepository<AuditLogDocument, String> {
}
