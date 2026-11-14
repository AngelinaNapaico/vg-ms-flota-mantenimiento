package pe.edu.vallegrande.caneteseguro.fleet.domain.port.in;

import pe.edu.vallegrande.caneteseguro.fleet.domain.model.MaintenanceOrder;
import pe.edu.vallegrande.caneteseguro.fleet.domain.model.TechnicalDischargeReport;
import pe.edu.vallegrande.caneteseguro.fleet.domain.model.WorkshopDetail;
import pe.edu.vallegrande.caneteseguro.fleet.domain.vo.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Inbound port (casos de uso) para el ciclo de mantenimiento de flota.
 * HEX-02, STD-022 & STD-033: Dominio puro nombrado como UseCase, libre de DTOs
 * e infraestructura.
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
