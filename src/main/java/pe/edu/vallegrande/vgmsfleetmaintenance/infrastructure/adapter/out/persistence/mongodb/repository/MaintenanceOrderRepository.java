package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.MaintenanceOrderDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Spring Data Reactive MongoDB repository for maintenance orders.
 * Provides reactive queries scoped by entity and status.
 */
@Repository
public interface MaintenanceOrderRepository extends ReactiveMongoRepository<MaintenanceOrderDocument, String> {

    Mono<MaintenanceOrderDocument> findByOrderCode(String orderCode);

    Mono<MaintenanceOrderDocument> findByEntityIdAndOrderCode(String entityId, String orderCode);

    Flux<MaintenanceOrderDocument> findByResourceId(String resourceId);

    Flux<MaintenanceOrderDocument> findByEntityIdAndResourceId(String entityId, String resourceId);

    Flux<MaintenanceOrderDocument> findByEntityId(String entityId);

    Flux<MaintenanceOrderDocument> findByOrderStatus(String orderStatus);

    Flux<MaintenanceOrderDocument> findByEntityIdAndOrderStatus(String entityId, String orderStatus);

    Mono<Boolean> existsByResourceIdAndOrderStatusNotIn(String resourceId, Collection<String> orderStatuses);

    Mono<Boolean> existsByResourceIdAndEntityIdAndOrderStatusNotIn(String resourceId, String entityId, Collection<String> orderStatuses);
}
