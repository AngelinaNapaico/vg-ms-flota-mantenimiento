package pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out;

import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.MaintenanceOrder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Outbound repository port for persistence of maintenance orders.
 */
public interface MaintenanceOrderRepositoryPort {

    Mono<MaintenanceOrder> findById(String id);

    Mono<MaintenanceOrder> findByIdAndEntityId(String id, String entityId);

    Mono<MaintenanceOrder> findByOrderCode(String orderCode);

    Mono<MaintenanceOrder> findByOrderCodeAndEntityId(String orderCode, String entityId);

    Flux<MaintenanceOrder> findByResourceId(String resourceId);

    Flux<MaintenanceOrder> findByResourceIdAndEntityId(String resourceId, String entityId);

    Flux<MaintenanceOrder> findByEntityId(String entityId);

    Flux<MaintenanceOrder> findByOrderStatus(String orderStatus);

    Flux<MaintenanceOrder> findByEntityIdAndOrderStatus(String entityId, String orderStatus);

    Flux<MaintenanceOrder> findAll();

    Mono<MaintenanceOrder> save(MaintenanceOrder order);

    Mono<Boolean> existsByResourceIdAndOrderStatusNotIn(String resourceId, Collection<String> statuses);

    Mono<Boolean> existsByResourceIdAndEntityIdAndOrderStatusNotIn(
            String resourceId, String entityId, Collection<String> statuses);
}
