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
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.SecurityContextPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of fleet maintenance lifecycle use cases.
 * Handles order creation, updates, workshop assignment, technical discharge,
 * and cancellation with audit logging and domain invariant enforcement.
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
    private final SecurityContextPort securityContextPort;

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
    public Mono<MaintenanceOrder> create(MaintenanceOrder order) {
        return create(order, null);
    }

    @Override
    public Mono<MaintenanceOrder> create(MaintenanceOrder order, String token) {
        log.info("Initiating creation of maintenance order for resource: {}", order.getResourceId());

        return resolveAuditContext().flatMap(ctx ->
                repositoryPort.existsByResourceIdAndOrderStatusNotIn(order.getResourceId(), TERMINAL_STATUSES)
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

                            Mono<Void> notifyResource = (token != null && !token.isBlank())
                                    ? resourceClientPort.disableResourceForMaintenance(order.getResourceId(), token)
                                    : resourceClientPort.disableResourceForMaintenance(order.getResourceId());

                            return notifyResource
                                    .onErrorResume(err -> {
                                        log.warn("Notice: Could not communicate directly with vg-ms-recursos (upstream unavailable), order will be saved locally. Cause: {}",
                                                err.getMessage());
                                        return Mono.empty();
                                    })
                                    .then(repositoryPort.save(order))
                                    .doOnSuccess(saved -> audit("CREATE", saved.getOrderCode(), saved.getResourceId(),
                                            ctx.actor(), saved.getEntityId() != null ? saved.getEntityId() : ctx.tenant(),
                                            "SUCCESS", ctx.correlationId(), null))
                                    .doOnError(err -> audit("CREATE", null, order.getResourceId(),
                                            ctx.actor(), ctx.tenant(), "FAILED", ctx.correlationId(), err));
                        })
        );
    }

    @Override
    public Mono<MaintenanceOrder> update(String id, MaintenanceOrder updatedDetails) {
        log.info("Updating editable header and workshop fields for maintenance order: {}", id);

        return resolveAuditContext().flatMap(ctx ->
                findById(id)
                        .flatMap(order -> {
                            order.updateDetails(
                                    updatedDetails.getFailureReason(),
                                    updatedDetails.getReportedMileage(),
                                    updatedDetails.getPriority(),
                                    updatedDetails.getRemarks(),
                                    updatedDetails.getIncidentId(),
                                    updatedDetails.getWorkshopDetail());
                            return repositoryPort.save(order)
                                    .doOnSuccess(saved -> audit("UPDATE", saved.getOrderCode(), saved.getResourceId(),
                                            ctx.actor(), saved.getEntityId() != null ? saved.getEntityId() : ctx.tenant(),
                                            "SUCCESS", ctx.correlationId(), null))
                                    .doOnError(err -> audit("UPDATE", id, null,
                                            ctx.actor(), ctx.tenant(), "FAILED", ctx.correlationId(), err));
                        })
        );
    }

    @Override
    public Mono<MaintenanceOrder> assignWorkshop(String id, WorkshopDetail workshop) {
        return resolveAuditContext().flatMap(ctx ->
                findById(id)
                        .flatMap(order -> {
                            order.assignWorkshop(workshop);
                            return repositoryPort.save(order)
                                    .doOnSuccess(saved -> audit("ASSIGN_WORKSHOP", saved.getOrderCode(), saved.getResourceId(),
                                            ctx.actor(), saved.getEntityId() != null ? saved.getEntityId() : ctx.tenant(),
                                            "SUCCESS", ctx.correlationId(), null))
                                    .doOnError(err -> audit("ASSIGN_WORKSHOP", id, null,
                                            ctx.actor(), ctx.tenant(), "FAILED", ctx.correlationId(), err));
                        })
        );
    }

    @Override
    public Mono<MaintenanceOrder> startRepair(String id) {
        return resolveAuditContext().flatMap(ctx ->
                findById(id)
                        .flatMap(order -> {
                            order.startRepair();
                            return repositoryPort.save(order)
                                    .doOnSuccess(saved -> audit("START_REPAIR", saved.getOrderCode(), saved.getResourceId(),
                                            ctx.actor(), saved.getEntityId() != null ? saved.getEntityId() : ctx.tenant(),
                                            "SUCCESS", ctx.correlationId(), null))
                                    .doOnError(err -> audit("START_REPAIR", id, null,
                                            ctx.actor(), ctx.tenant(), "FAILED", ctx.correlationId(), err));
                        })
        );
    }

    @Override
    public Mono<MaintenanceOrder> passToQualityControl(String id) {
        return resolveAuditContext().flatMap(ctx ->
                findById(id)
                        .flatMap(order -> {
                            order.passToQualityControl();
                            return repositoryPort.save(order)
                                    .doOnSuccess(saved -> audit("QUALITY_CONTROL", saved.getOrderCode(), saved.getResourceId(),
                                            ctx.actor(), saved.getEntityId() != null ? saved.getEntityId() : ctx.tenant(),
                                            "SUCCESS", ctx.correlationId(), null))
                                    .doOnError(err -> audit("QUALITY_CONTROL", id, null,
                                            ctx.actor(), ctx.tenant(), "FAILED", ctx.correlationId(), err));
                        })
        );
    }

    @Override
    public Mono<MaintenanceOrder> issueTechnicalDischarge(String id, TechnicalDischargeReport dischargeReport) {
        return issueTechnicalDischarge(id, dischargeReport, null);
    }

    @Override
    public Mono<MaintenanceOrder> issueTechnicalDischarge(String id, TechnicalDischargeReport dischargeReport, String token) {
        log.info("Issuing technical discharge for order: {}", id);

        return resolveAuditContext().flatMap(ctx ->
                findById(id)
                        .flatMap(order -> {
                            order.issueTechnicalDischarge(dischargeReport);

                            Mono<Void> notifyResource = (token != null && !token.isBlank())
                                    ? resourceClientPort.enableResourceToAvailable(order.getResourceId(), token)
                                    : resourceClientPort.enableResourceToAvailable(order.getResourceId());

                            return notifyResource
                                    .onErrorResume(err -> {
                                        log.warn("Notice: Could not update resource availability in vg-ms-recursos (upstream unavailable), order completed locally. Cause: {}",
                                                err.getMessage());
                                        return Mono.empty();
                                    })
                                    .then(repositoryPort.save(order))
                                    .doOnSuccess(saved -> audit("TECHNICAL_DISCHARGE", saved.getOrderCode(), saved.getResourceId(),
                                            ctx.actor(), saved.getEntityId() != null ? saved.getEntityId() : ctx.tenant(),
                                            "SUCCESS", ctx.correlationId(), null))
                                    .doOnError(err -> audit("TECHNICAL_DISCHARGE", id, null,
                                            ctx.actor(), ctx.tenant(), "FAILED", ctx.correlationId(), err));
                        })
        );
    }

    @Override
    public Mono<MaintenanceOrder> cancel(String id, String reason, String remarks) {
        return cancel(id, reason, remarks, null);
    }

    @Override
    public Mono<MaintenanceOrder> cancel(String id, String reason, String remarks, String token) {
        log.info("Canceling maintenance order: {}", id);

        return resolveAuditContext().flatMap(ctx ->
                findById(id)
                        .flatMap(order -> {
                            order.cancel(reason);

                            Mono<Void> notifyResource = (token != null && !token.isBlank())
                                    ? resourceClientPort.enableResourceToAvailable(order.getResourceId(), token)
                                    : resourceClientPort.enableResourceToAvailable(order.getResourceId());

                            return notifyResource
                                    .onErrorResume(err -> {
                                        log.warn("Notice: Could not restore resource availability in vg-ms-recursos (upstream unavailable), order cancelled locally. Cause: {}",
                                                err.getMessage());
                                        return Mono.empty();
                                    })
                                    .then(repositoryPort.save(order))
                                    .doOnSuccess(saved -> audit("CANCEL", saved.getOrderCode(), saved.getResourceId(),
                                            ctx.actor(), saved.getEntityId() != null ? saved.getEntityId() : ctx.tenant(),
                                            "SUCCESS", ctx.correlationId(), null))
                                    .doOnError(err -> audit("CANCEL", id, null,
                                            ctx.actor(), ctx.tenant(), "FAILED", ctx.correlationId(), err));
                        })
        );
    }

    private void audit(String action, String orderCode, String resourceId, String actor, String tenant,
                       String result, String correlationId, Throwable error) {
        String errorName = error != null ? error.getClass().getSimpleName() : "NONE";
        log.info("AUDIT: Timestamp={}, Action={}, Order={}, Resource={}, Actor={}, Tenant={}, Result={}, CorrelationId={}, Error={}",
                Instant.now(), action,
                orderCode != null ? orderCode : "N/A",
                resourceId != null ? resourceId : "N/A",
                actor != null ? actor : "ANONYMOUS",
                tenant != null ? tenant : "UNKNOWN",
                result,
                correlationId != null ? correlationId : UUID.randomUUID().toString(),
                errorName);
    }

    private Mono<AuditContext> resolveAuditContext() {
        return Mono.zip(
                securityContextPort.getCurrentUsername().defaultIfEmpty("ANONYMOUS"),
                securityContextPort.getCurrentEntityId().defaultIfEmpty("UNKNOWN")
        ).map(tuple -> new AuditContext(tuple.getT1(), tuple.getT2(), UUID.randomUUID().toString()));
    }

    private record AuditContext(String actor, String tenant, String correlationId) {}
}
