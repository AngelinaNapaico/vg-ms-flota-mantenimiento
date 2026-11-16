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

    Mono<MaintenanceOrder> findByOrderCode(String orderCode);

    Flux<MaintenanceOrder> findByResourceId(String resourceId);

    Flux<MaintenanceOrder> findByEntityId(String entityId);

    Flux<MaintenanceOrder> findByOrderStatus(String orderStatus);

    Flux<MaintenanceOrder> findAll();

    Mono<MaintenanceOrder> save(MaintenanceOrder order);

    Mono<Boolean> existsByResourceIdAndOrderStatusNotIn(String resourceId, Collection<String> statuses);
}
