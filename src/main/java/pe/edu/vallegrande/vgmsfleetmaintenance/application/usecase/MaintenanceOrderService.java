package pe.edu.vallegrande.vgmsfleetmaintenance.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.MaintenanceOrder;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.TechnicalDischargeReport;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.WorkshopDetail;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.in.MaintenanceOrderUseCase;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.MaintenanceOrderRepositoryPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderStatus;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security.SecurityUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of fleet maintenance lifecycle use cases.
 * Fully compliant with Hexagonal Architecture, DDD, STD-074 and SEC-019:
 * - Emits structured audit events on all mutations (CREATE, UPDATE, DISCHARGE, CANCEL).
 * - Sanitizes upstream external service error logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceOrderService implements MaintenanceOrderUseCase {

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

        return SecurityUtils.getCurrentUsername()
                .defaultIfEmpty("SYSTEM")
                .flatMap(actor -> repositoryPort.existsByResourceIdAndOrderStatusNotIn(order.getResourceId(), TERMINAL_STATUSES)
                        .flatMap(existsActive -> {
                            if (Boolean.TRUE.equals(existsActive)) {
                                log.warn("Operational resource {} already has an active maintenance order", order.getResourceId());
                                return Mono.error(new DomainException("Operational resource already has an active maintenance order"));
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
                                        log.warn("Notice: Could not communicate directly with vg-ms-recursos (upstream unavailable), order will be saved locally.");
                                        return Mono.empty();
                                    })
                                    .then(repositoryPort.save(order))
                                    .doOnSuccess(saved -> log.info("AUDIT: Action=CREATE, Order={}, Resource={}, Actor={}, Tenant={}, Result=SUCCESS",
                                            saved.getOrderCode(), saved.getResourceId(), actor, saved.getEntityId()))
                                    .doOnError(err -> log.warn("AUDIT: Action=CREATE, Resource={}, Actor={}, Result=FAILED, Error={}",
                                            order.getResourceId(), actor, err.getClass().getSimpleName()));
                        }));
    }

    @Override
    public Mono<MaintenanceOrder> update(String id, MaintenanceOrder updatedDetails) {
        log.info("Updating editable header and workshop fields for maintenance order: {}", id);

        return SecurityUtils.getCurrentUsername()
                .defaultIfEmpty("SYSTEM")
                .flatMap(actor -> findById(id)
                        .flatMap(order -> {
                            order.updateDetails(
                                    updatedDetails.getFailureReason(),
                                    updatedDetails.getReportedMileage(),
                                    updatedDetails.getPriority(),
                                    updatedDetails.getRemarks(),
                                    updatedDetails.getIncidentId(),
                                    updatedDetails.getWorkshopDetail());
                            return repositoryPort.save(order)
                                    .doOnSuccess(saved -> log.info("AUDIT: Action=UPDATE, Order={}, Actor={}, Tenant={}, Result=SUCCESS",
                                            saved.getOrderCode(), actor, saved.getEntityId()))
                                    .doOnError(err -> log.warn("AUDIT: Action=UPDATE, Order={}, Actor={}, Result=FAILED, Error={}",
                                            id, actor, err.getClass().getSimpleName()));
                        }));
    }

    @Override
    public Mono<MaintenanceOrder> assignWorkshop(String id, WorkshopDetail workshop) {
        return SecurityUtils.getCurrentUsername()
                .defaultIfEmpty("SYSTEM")
                .flatMap(actor -> findById(id)
                        .flatMap(order -> {
                            order.assignWorkshop(workshop);
                            return repositoryPort.save(order)
                                    .doOnSuccess(saved -> log.info("AUDIT: Action=ASSIGN_WORKSHOP, Order={}, Actor={}, Result=SUCCESS",
                                            saved.getOrderCode(), actor));
                        }));
    }

    @Override
    public Mono<MaintenanceOrder> startRepair(String id) {
        return SecurityUtils.getCurrentUsername()
                .defaultIfEmpty("SYSTEM")
                .flatMap(actor -> findById(id)
                        .flatMap(order -> {
                            order.startRepair();
                            return repositoryPort.save(order)
                                    .doOnSuccess(saved -> log.info("AUDIT: Action=START_REPAIR, Order={}, Actor={}, Result=SUCCESS",
                                            saved.getOrderCode(), actor));
                        }));
    }

    @Override
    public Mono<MaintenanceOrder> passToQualityControl(String id) {
        return SecurityUtils.getCurrentUsername()
                .defaultIfEmpty("SYSTEM")
                .flatMap(actor -> findById(id)
                        .flatMap(order -> {
                            order.passToQualityControl();
                            return repositoryPort.save(order)
                                    .doOnSuccess(saved -> log.info("AUDIT: Action=QUALITY_CONTROL, Order={}, Actor={}, Result=SUCCESS",
                                            saved.getOrderCode(), actor));
                        }));
    }

    @Override
    public Mono<MaintenanceOrder> issueTechnicalDischarge(String id, TechnicalDischargeReport dischargeReport, String token) {
        log.info("Issuing technical discharge for order: {}", id);

        return SecurityUtils.getCurrentUsername()
                .defaultIfEmpty("SYSTEM")
                .flatMap(actor -> findById(id)
                        .flatMap(order -> {
                            order.issueTechnicalDischarge(dischargeReport);

                            return resourceClientPort.enableResourceToAvailable(order.getResourceId(), token)
                                    .onErrorResume(err -> {
                                        log.warn("Notice: Could not update resource availability in vg-ms-recursos (upstream unavailable), order completed locally.");
                                        return Mono.empty();
                                    })
                                    .then(repositoryPort.save(order))
                                    .doOnSuccess(saved -> log.info("AUDIT: Action=TECHNICAL_DISCHARGE, Order={}, Resource={}, Actor={}, Tenant={}, Result=SUCCESS",
                                            saved.getOrderCode(), saved.getResourceId(), actor, saved.getEntityId()))
                                    .doOnError(err -> log.warn("AUDIT: Action=TECHNICAL_DISCHARGE, Order={}, Actor={}, Result=FAILED, Error={}",
                                            id, actor, err.getClass().getSimpleName()));
                        }));
    }

    @Override
    public Mono<MaintenanceOrder> cancel(String id, String reason, String remarks, String token) {
        log.info("Canceling maintenance order: {}", id);

        return SecurityUtils.getCurrentUsername()
                .defaultIfEmpty("SYSTEM")
                .flatMap(actor -> findById(id)
                        .flatMap(order -> {
                            order.cancel(reason);

                            return resourceClientPort.enableResourceToAvailable(order.getResourceId(), token)
                                    .onErrorResume(err -> {
                                        log.warn("Notice: Could not restore resource availability in vg-ms-recursos (upstream unavailable), order cancelled locally.");
                                        return Mono.empty();
                                    })
                                    .then(repositoryPort.save(order))
                                    .doOnSuccess(saved -> log.info("AUDIT: Action=CANCEL, Order={}, Resource={}, Actor={}, Tenant={}, Result=SUCCESS",
                                            saved.getOrderCode(), saved.getResourceId(), actor, saved.getEntityId()))
                                    .doOnError(err -> log.warn("AUDIT: Action=CANCEL, Order={}, Actor={}, Result=FAILED, Error={}",
                                            id, actor, err.getClass().getSimpleName()));
                        }));
    }
}
