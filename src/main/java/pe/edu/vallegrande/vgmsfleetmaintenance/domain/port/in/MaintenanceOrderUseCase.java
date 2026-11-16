package pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.in;

import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.MaintenanceOrder;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.TechnicalDischargeReport;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.WorkshopDetail;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Inbound port defining use case operations for the fleet maintenance lifecycle.
 */
public interface MaintenanceOrderUseCase {

    Flux<MaintenanceOrder> findAll();

    Mono<MaintenanceOrder> findById(String id);

    Mono<MaintenanceOrder> findByCode(String code);

    Flux<MaintenanceOrder> findByResource(String resourceId);

    Flux<MaintenanceOrder> findByEntity(String entityId);

    Flux<MaintenanceOrder> findByStatus(OrderStatus status);

    Mono<MaintenanceOrder> create(MaintenanceOrder order, String token);

    Mono<MaintenanceOrder> update(String id, MaintenanceOrder order);

    Mono<MaintenanceOrder> assignWorkshop(String id, WorkshopDetail workshop);

    Mono<MaintenanceOrder> startRepair(String id);

    Mono<MaintenanceOrder> passToQualityControl(String id);

    Mono<MaintenanceOrder> issueTechnicalDischarge(String id, TechnicalDischargeReport dischargeReport, String token);

    Mono<MaintenanceOrder> cancel(String id, String reason, String remarks, String token);
}
