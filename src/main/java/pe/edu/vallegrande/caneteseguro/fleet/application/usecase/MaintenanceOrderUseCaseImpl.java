package pe.edu.vallegrande.caneteseguro.fleet.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.caneteseguro.fleet.domain.exception.DomainException;
import pe.edu.vallegrande.caneteseguro.fleet.domain.model.MaintenanceOrder;
import pe.edu.vallegrande.caneteseguro.fleet.domain.model.TechnicalDischargeReport;
import pe.edu.vallegrande.caneteseguro.fleet.domain.model.WorkshopDetail;
import pe.edu.vallegrande.caneteseguro.fleet.domain.port.in.MaintenanceOrderUseCase;
import pe.edu.vallegrande.caneteseguro.fleet.domain.port.out.MaintenanceOrderRepositoryPort;
import pe.edu.vallegrande.caneteseguro.fleet.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.caneteseguro.fleet.domain.vo.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of fleet maintenance lifecycle use cases.
 * 100% in English according to DatabaseStorage, Hexagonal and STD-077
 * standards.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceOrderUseCaseImpl implements MaintenanceOrderUseCase {

    private static final List<String> TERMINAL_STATUSES = List.of(
            OrderStatus.COMPLETED.name(),
            OrderStatus.CANCELLED.name(),
            OrderStatus.COMPLETED.getExternalCode(),
            OrderStatus.CANCELLED.getExternalCode());

    private final MaintenanceOrderRepositoryPort repositoryPort;
    private final ResourceClientPort resourceClientPort;

    @Override
    public Flux<MaintenanceOrder> findAll() {
        return repositoryPort.findAll();
    }

    @Override
    public Mono<MaintenanceOrder> findById(String id) {
        return repositoryPort.findById(id)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Maintenance order not found with ID: {}", id);
                    return Mono.error(new DomainException("Maintenance order not found"));
                }));
    }

    @Override
    public Mono<MaintenanceOrder> findByCode(String code) {
        return repositoryPort.findByOrderCode(code)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Maintenance order not found with code: {}", code);
                    return Mono.error(new DomainException("Maintenance order not found"));
                }));
    }

    @Override
    public Flux<MaintenanceOrder> findByResource(String resourceId) {
        return repositoryPort.findByResourceId(resourceId);
    }

    @Override
    public Flux<MaintenanceOrder> findByEntity(String entityId) {
        return repositoryPort.findByEntityId(entityId);
    }

    @Override
    public Flux<MaintenanceOrder> findByStatus(OrderStatus status) {
        return repositoryPort.findByOrderStatus(status.name());
    }

    @Override
    public Mono<MaintenanceOrder> create(MaintenanceOrder order, String token) {
        log.info("Initiating creation of maintenance order for resource: {}", order.getResourceId());

        return repositoryPort.existsByResourceIdAndOrderStatusNotIn(order.getResourceId(), TERMINAL_STATUSES)
                .flatMap(existsActive -> {
                    if (Boolean.TRUE.equals(existsActive)) {
                        log.warn("Operational resource {} already has an active maintenance order",
                                order.getResourceId());
                        return Mono.error(
                                new DomainException("Operational resource already has an active maintenance order"));
                    }

                    String year = String.valueOf(LocalDateTime.now().getYear());
                    String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                    String orderCode = "ORD-" + year + "-" + suffix;

                    order.setOrderCode(orderCode);
                    order.setOrderStatus(OrderStatus.REGISTERED);
                    order.setStatus("A");
                    order.setCreatedAt(LocalDateTime.now());
                    order.setUpdatedAt(LocalDateTime.now());

                    return resourceClientPort.disableResourceForMaintenance(order.getResourceId(), token)
                            .onErrorResume(err -> {
                                log.warn(
                                        "Notice: Could not communicate directly with vg-ms-recursos ({}), order will be saved locally.",
                                        err.getMessage());
                                return Mono.empty();
                            })
                            .then(repositoryPort.save(order));
                });
    }

    @Override
    public Mono<MaintenanceOrder> update(String id, MaintenanceOrder updatedDetails) {
        log.info("Updating editable header and workshop fields for maintenance order: {}", id);
        return findById(id)
                .flatMap(order -> {
                    order.updateDetails(
                            updatedDetails.getFailureReason(),
                            updatedDetails.getReportedMileage(),
                            updatedDetails.getPriority(),
                            updatedDetails.getRemarks(),
                            updatedDetails.getIncidentId(),
                            updatedDetails.getWorkshopDetail());
                    return repositoryPort.save(order);
                });
    }

    @Override
    public Mono<MaintenanceOrder> assignWorkshop(String id, WorkshopDetail workshop) {
        return findById(id)
                .flatMap(order -> {
                    order.assignWorkshop(workshop);
                    return repositoryPort.save(order);
                });
    }

    @Override
    public Mono<MaintenanceOrder> startRepair(String id) {
        return findById(id)
                .flatMap(order -> {
                    order.startRepair();
                    return repositoryPort.save(order);
                });
    }

    @Override
    public Mono<MaintenanceOrder> passToQualityControl(String id) {
        return findById(id)
                .flatMap(order -> {
                    order.passToQualityControl();
                    return repositoryPort.save(order);
                });
    }

    @Override
    public Mono<MaintenanceOrder> issueTechnicalDischarge(String id, TechnicalDischargeReport dischargeReport,
            String token) {
        log.info("Issuing technical discharge for order: {}", id);
        return findById(id)
                .flatMap(order -> {
                    order.issueTechnicalDischarge(dischargeReport);

                    return resourceClientPort.enableResourceToAvailable(order.getResourceId(), token)
                            .onErrorResume(err -> {
                                log.warn(
                                        "Notice: Could not update resource availability in vg-ms-recursos ({}), order completed locally.",
                                        err.getMessage());
                                return Mono.empty();
                            })
                            .then(repositoryPort.save(order));
                });
    }

    @Override
    public Mono<MaintenanceOrder> cancel(String id, String reason, String remarks, String token) {
        log.info("Canceling maintenance order: {}", id);
        return findById(id)
                .flatMap(order -> {
                    order.cancel(reason);

                    return resourceClientPort.enableResourceToAvailable(order.getResourceId(), token)
                            .onErrorResume(err -> {
                                log.warn(
                                        "Notice: Could not restore resource availability in vg-ms-recursos ({}), order cancelled locally.",
                                        err.getMessage());
                                return Mono.empty();
                            })
                            .then(repositoryPort.save(order));
                });
    }
}
