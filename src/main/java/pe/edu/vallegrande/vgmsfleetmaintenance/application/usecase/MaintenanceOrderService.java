package pe.edu.vallegrande.vgmsfleetmaintenance.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.MaintenanceOrder;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.TechnicalDischargeReport;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.WorkshopDetail;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.in.MaintenanceOrderUseCase;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.AuditLogRepositoryPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.AuthClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.MaintenanceOrderRepositoryPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of fleet maintenance lifecycle use cases.
 * Handles order creation, updates, workshop assignment, technical discharge,
 * and cancellation with comprehensive audit logging and domain invariant enforcement.
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
    private final AuthClientPort authClientPort;
    private final AuditLogRepositoryPort auditLogRepositoryPort;

    @Override
    public Flux<MaintenanceOrder> findAll() {
        return repositoryPort.findAll();
    }

    @Override
    public Mono<MaintenanceOrder> findById(String id) {
        return repositoryPort.findById(id)
                .switchIfEmpty(Mono.error(new DomainException("Maintenance order not found: " + id)));
    }

    @Override
    public Mono<MaintenanceOrder> findByCode(String code) {
        return repositoryPort.findByOrderCode(code)
                .switchIfEmpty(Mono.error(new DomainException("Maintenance order not found with code: " + code)));
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
        String correlationId = UUID.randomUUID().toString();

        return resolveCurrentActor().flatMap(actor ->
                resolveCurrentTenant().flatMap(tenant -> {
                    String effectiveTenant = order.getEntityId() != null && !order.getEntityId().isBlank()
                            ? order.getEntityId()
                            : (tenant.isBlank() ? null : tenant);

                    return resourceClientPort
                            .validateResourceBelongsToEntity(order.getResourceId(), effectiveTenant, token)
                            .then(repositoryPort.existsByResourceIdAndOrderStatusNotIn(order.getResourceId(),
                                    TERMINAL_STATUSES))
                            .flatMap(existsActive -> {
                                if (Boolean.TRUE.equals(existsActive)) {
                                    log.warn("Operational resource {} already has an active maintenance order",
                                            order.getResourceId());
                                    return Mono.error(new DomainException(
                                            "Operational resource already has an active maintenance order"));
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
                                        ? resourceClientPort.disableResourceForMaintenance(
                                        order.getResourceId(), token)
                                        : resourceClientPort.disableResourceForMaintenance(
                                        order.getResourceId());

                                return notifyResource
                                        .onErrorResume(err -> {
                                            log.warn("Notice: Could not communicate directly with vg-ms-recursos (upstream unavailable), order will be saved locally. Type: {}",
                                                    err.getClass().getSimpleName());
                                            return Mono.empty();
                                        })
                                        .then(repositoryPort.save(order))
                                        .flatMap(saved -> audit("CREATE", saved.getOrderCode(), saved.getResourceId(),
                                                actor, saved.getEntityId(), "SUCCESS", correlationId, null)
                                                .thenReturn(saved));
                            })
                            .onErrorResume(err -> audit("CREATE", null, order.getResourceId(),
                                    actor, effectiveTenant, "FAILED", correlationId, err)
                                    .then(Mono.error(err)));
                }));
    }

    @Override
    public Mono<MaintenanceOrder> update(String id, MaintenanceOrder updatedDetails) {
        log.info("Updating editable header and workshop fields for maintenance order: {}", id);
        String correlationId = UUID.randomUUID().toString();

        return resolveCurrentActor().flatMap(actor -> findById(id)
                .flatMap(order -> Mono.fromCallable(() -> {
                    order.updateDetails(
                            updatedDetails.getFailureReason(),
                            updatedDetails.getReportedMileage(),
                            updatedDetails.getPriority(),
                            updatedDetails.getRemarks(),
                            updatedDetails.getIncidentId(),
                            updatedDetails.getWorkshopDetail());
                    return order;
                }).flatMap(repositoryPort::save)
                        .flatMap(saved -> audit("UPDATE", saved.getOrderCode(), saved.getResourceId(),
                                actor, saved.getEntityId(), "SUCCESS", correlationId, null)
                                .thenReturn(saved)))
                .onErrorResume(err -> audit("UPDATE", id, null, actor, null, "FAILED", correlationId, err)
                        .then(Mono.error(err))));
    }

    @Override
    public Mono<MaintenanceOrder> assignWorkshop(String id, WorkshopDetail workshop) {
        String correlationId = UUID.randomUUID().toString();

        return resolveCurrentActor().flatMap(actor -> findById(id)
                .flatMap(order -> Mono.fromCallable(() -> {
                    order.assignWorkshop(workshop);
                    return order;
                }).flatMap(repositoryPort::save)
                        .flatMap(saved -> audit("ASSIGN_WORKSHOP", saved.getOrderCode(), saved.getResourceId(),
                                actor, saved.getEntityId(), "SUCCESS", correlationId, null)
                                .thenReturn(saved)))
                .onErrorResume(err -> audit("ASSIGN_WORKSHOP", id, null, actor, null, "FAILED", correlationId, err)
                        .then(Mono.error(err))));
    }

    @Override
    public Mono<MaintenanceOrder> startRepair(String id) {
        String correlationId = UUID.randomUUID().toString();

        return resolveCurrentActor().flatMap(actor -> findById(id)
                .flatMap(order -> Mono.fromCallable(() -> {
                    order.startRepair();
                    return order;
                }).flatMap(repositoryPort::save)
                        .flatMap(saved -> audit("START_REPAIR", saved.getOrderCode(), saved.getResourceId(),
                                actor, saved.getEntityId(), "SUCCESS", correlationId, null)
                                .thenReturn(saved)))
                .onErrorResume(err -> audit("START_REPAIR", id, null, actor, null, "FAILED", correlationId, err)
                        .then(Mono.error(err))));
    }

    @Override
    public Mono<MaintenanceOrder> passToQualityControl(String id) {
        String correlationId = UUID.randomUUID().toString();

        return resolveCurrentActor().flatMap(actor -> findById(id)
                .flatMap(order -> Mono.fromCallable(() -> {
                    order.passToQualityControl();
                    return order;
                }).flatMap(repositoryPort::save)
                        .flatMap(saved -> audit("QUALITY_CONTROL", saved.getOrderCode(), saved.getResourceId(),
                                actor, saved.getEntityId(), "SUCCESS", correlationId, null)
                                .thenReturn(saved)))
                .onErrorResume(err -> audit("QUALITY_CONTROL", id, null, actor, null, "FAILED", correlationId, err)
                        .then(Mono.error(err))));
    }

    @Override
    public Mono<MaintenanceOrder> issueTechnicalDischarge(String id, TechnicalDischargeReport dischargeReport) {
        return issueTechnicalDischarge(id, dischargeReport, null);
    }

    @Override
    public Mono<MaintenanceOrder> issueTechnicalDischarge(String id, TechnicalDischargeReport dischargeReport,
                                                          String token) {
        log.info("Issuing technical discharge for order: {}", id);
        String correlationId = UUID.randomUUID().toString();

        return resolveCurrentActor().flatMap(actor -> findById(id)
                .flatMap(order -> Mono.fromCallable(() -> {
                    order.issueTechnicalDischarge(dischargeReport);
                    return order;
                }).flatMap(modifiedOrder -> {
                    Mono<Void> notifyResource = (token != null && !token.isBlank())
                            ? resourceClientPort.enableResourceToAvailable(
                            modifiedOrder.getResourceId(), token)
                            : resourceClientPort.enableResourceToAvailable(
                            modifiedOrder.getResourceId());

                    return notifyResource
                            .onErrorResume(err -> {
                                log.warn("Notice: Could not update resource availability in vg-ms-recursos (upstream unavailable), order completed locally. Type: {}",
                                        err.getClass().getSimpleName());
                                return Mono.empty();
                            })
                            .then(repositoryPort.save(modifiedOrder))
                            .flatMap(saved -> audit("TECHNICAL_DISCHARGE", saved.getOrderCode(), saved.getResourceId(),
                                    actor, saved.getEntityId(), "SUCCESS", correlationId, null)
                                    .thenReturn(saved));
                }))
                .onErrorResume(err -> audit("TECHNICAL_DISCHARGE", id, null, actor, null, "FAILED", correlationId, err)
                        .then(Mono.error(err))));
    }

    @Override
    public Mono<MaintenanceOrder> cancel(String id, String reason, String remarks) {
        return cancel(id, reason, remarks, null);
    }

    @Override
    public Mono<MaintenanceOrder> cancel(String id, String reason, String remarks, String token) {
        log.info("Canceling maintenance order: {}", id);
        String correlationId = UUID.randomUUID().toString();

        return resolveCurrentActor().flatMap(actor -> findById(id)
                .flatMap(order -> Mono.fromCallable(() -> {
                    order.cancel(reason);
                    return order;
                }).flatMap(cancelledOrder -> {
                    Mono<Void> notifyResource = (token != null && !token.isBlank())
                            ? resourceClientPort.enableResourceToAvailable(
                            cancelledOrder.getResourceId(), token)
                            : resourceClientPort.enableResourceToAvailable(
                            cancelledOrder.getResourceId());

                    return notifyResource
                            .onErrorResume(err -> {
                                log.warn("Notice: Could not restore resource availability in vg-ms-recursos (upstream unavailable), order cancelled locally. Type: {}",
                                        err.getClass().getSimpleName());
                                return Mono.empty();
                            })
                            .then(repositoryPort.save(cancelledOrder))
                            .flatMap(saved -> audit("CANCEL", saved.getOrderCode(), saved.getResourceId(),
                                    actor, saved.getEntityId(), "SUCCESS", correlationId, null)
                                    .thenReturn(saved));
                }))
                .onErrorResume(err -> audit("CANCEL", id, null, actor, null, "FAILED", correlationId, err)
                        .then(Mono.error(err))));
    }

    private Mono<Void> audit(String action, String orderCode, String resourceId, String actor, String tenant,
                             String result, String correlationId, Throwable error) {
        String errorName = error != null ? error.getClass().getSimpleName() : "NONE";
        log.info("AUDIT: Timestamp={}, Action={}, Order={}, Resource={}, Actor={}, Tenant={}, Result={}, CorrelationId={}, Error={}",
                Instant.now(), action,
                orderCode != null ? orderCode : "N/A",
                resourceId != null ? resourceId : "N/A",
                actor != null ? actor : "ANONYMOUS",
                tenant != null ? tenant : "N/A",
                result,
                correlationId != null ? correlationId : UUID.randomUUID().toString(),
                errorName);

        if (auditLogRepositoryPort != null) {
            return auditLogRepositoryPort
                    .recordAudit(action, orderCode, resourceId, actor, tenant, result, correlationId, errorName)
                    .onErrorResume(err -> {
                        log.warn("Notice: Failed to persist durable audit log: {}", err.getClass().getSimpleName());
                        return Mono.empty();
                    });
        }
        return Mono.empty();
    }

    private Mono<String> resolveCurrentActor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> {
                    if (auth instanceof JwtAuthenticationToken jwt) {
                        Map<String, Object> claims = jwt.getTokenAttributes();
                        if (claims.get("preferred_username") != null) {
                            return claims.get("preferred_username").toString();
                        }
                        if (claims.get("sub") != null) {
                            return claims.get("sub").toString();
                        }
                    }
                    return auth.getName() != null ? auth.getName() : "ANONYMOUS";
                })
                .switchIfEmpty(authClientPort.getCurrentUsername())
                .defaultIfEmpty("ANONYMOUS");
    }

    private Mono<String> resolveCurrentTenant() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    if (auth instanceof JwtAuthenticationToken jwt) {
                        Object eid = jwt.getTokenAttributes().get("eid");
                        if (eid != null && !eid.toString().isBlank()) {
                            return Mono.just(eid.toString());
                        }
                    }
                    return Mono.empty();
                })
                .switchIfEmpty(authClientPort.getCurrentEntityId())
                .defaultIfEmpty("");
    }
}
