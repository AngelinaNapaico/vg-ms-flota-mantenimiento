package pe.edu.vallegrande.vgmsfleetmaintenance.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.vgmsfleetmaintenance.application.dto.request.RecordAuditLogRequest;
import pe.edu.vallegrande.vgmsfleetmaintenance.application.dto.response.AuditLogResponse;
import pe.edu.vallegrande.vgmsfleetmaintenance.application.mapper.AuditLogMapper;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of fleet maintenance lifecycle use cases.
 * Handles order creation, updates, workshop assignment, technical discharge,
 * and cancellation with mandatory durable audit logging and strict tenant
 * enforcement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceOrderService implements MaintenanceOrderUseCase {

        private static final String AUDIT_PERSISTENCE_FAILURE_MESSAGE = "Failed to persist durable audit log for operation";

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
                return authClientPort.hasGlobalAccess()
                                .defaultIfEmpty(false)
                                .flatMapMany(isGlobal -> {
                                        if (Boolean.TRUE.equals(isGlobal)) {
                                                return repositoryPort.findAll();
                                        }
                                        return authClientPort.getCurrentEntityId()
                                                        .filter(entityId -> !entityId.isBlank())
                                                        .flatMapMany(repositoryPort::findByEntityId);
                                });
        }

        @Override
        public Mono<MaintenanceOrder> findById(String id) {
                return authClientPort.hasGlobalAccess()
                                .defaultIfEmpty(false)
                                .flatMap(isGlobal -> {
                                        if (Boolean.TRUE.equals(isGlobal)) {
                                                return repositoryPort.findById(id);
                                        }
                                        return authClientPort.getCurrentEntityId()
                                                        .filter(entityId -> !entityId.isBlank())
                                                        .flatMap(entityId -> repositoryPort.findByIdAndEntityId(id,
                                                                        entityId));
                                })
                                .switchIfEmpty(Mono.error(
                                                DomainException.notFound("Requested maintenance order was not found")));
        }

        @Override
        public Mono<MaintenanceOrder> findByCode(String code) {
                return authClientPort.hasGlobalAccess()
                                .defaultIfEmpty(false)
                                .flatMap(isGlobal -> {
                                        if (Boolean.TRUE.equals(isGlobal)) {
                                                return repositoryPort.findByOrderCode(code);
                                        }
                                        return authClientPort.getCurrentEntityId()
                                                        .filter(entityId -> !entityId.isBlank())
                                                        .flatMap(entityId -> repositoryPort
                                                                        .findByOrderCodeAndEntityId(code, entityId));
                                })
                                .switchIfEmpty(Mono.error(
                                                DomainException.notFound("Requested maintenance order was not found")));
        }

        @Override
        public Flux<MaintenanceOrder> findByResource(String resourceId) {
                return authClientPort.hasGlobalAccess()
                                .defaultIfEmpty(false)
                                .flatMapMany(isGlobal -> {
                                        if (Boolean.TRUE.equals(isGlobal)) {
                                                return repositoryPort.findByResourceId(resourceId);
                                        }
                                        return authClientPort.getCurrentEntityId()
                                                        .filter(entityId -> !entityId.isBlank())
                                                        .flatMapMany(entityId -> repositoryPort
                                                                        .findByResourceIdAndEntityId(resourceId,
                                                                                        entityId));
                                });
        }

        @Override
        public Flux<MaintenanceOrder> findByEntity(String entityId) {
                return authClientPort.hasGlobalAccess()
                                .defaultIfEmpty(false)
                                .flatMapMany(isGlobal -> {
                                        if (Boolean.TRUE.equals(isGlobal)) {
                                                return repositoryPort.findByEntityId(entityId);
                                        }
                                        return authClientPort.getCurrentEntityId()
                                                        .filter(callerEntityId -> !callerEntityId.isBlank()
                                                                        && callerEntityId.equals(entityId))
                                                        .flatMapMany(repositoryPort::findByEntityId);
                                });
        }

        @Override
        public Flux<MaintenanceOrder> findByStatus(OrderStatus status) {
                return authClientPort.hasGlobalAccess()
                                .defaultIfEmpty(false)
                                .flatMapMany(isGlobal -> {
                                        if (Boolean.TRUE.equals(isGlobal)) {
                                                return repositoryPort.findByOrderStatus(status.name());
                                        }
                                        return authClientPort.getCurrentEntityId()
                                                        .filter(entityId -> !entityId.isBlank())
                                                        .flatMapMany(entityId -> repositoryPort
                                                                        .findByEntityIdAndOrderStatus(entityId,
                                                                                        status.name()));
                                });
        }

        @Override
        public Mono<MaintenanceOrder> create(MaintenanceOrder order) {
                return create(order, null);
        }

        @Override
        public Mono<MaintenanceOrder> create(MaintenanceOrder order, String token) {
                log.info("Initiating creation of maintenance order");
                String correlationId = UUID.randomUUID().toString();
                AtomicReference<String> effectiveTenantRef = new AtomicReference<>();

                return resolveCurrentActor().flatMap(
                                actor -> resolveEffectiveEntityId(order.getEntityId()).flatMap(effectiveTenant -> {
                                        effectiveTenantRef.set(effectiveTenant);
                                        order.setEntityId(effectiveTenant);

                                        Mono<Void> validateResource = (token != null && !token.isBlank())
                                                        ? resourceClientPort.validateResourceBelongsToEntity(
                                                                        order.getResourceId(), effectiveTenant, token)
                                                        : resourceClientPort.validateResourceBelongsToEntity(
                                                                        order.getResourceId(), effectiveTenant);

                                        return validateResource
                                                        .then(repositoryPort
                                                                        .existsByResourceIdAndEntityIdAndOrderStatusNotIn(
                                                                                        order.getResourceId(),
                                                                                        effectiveTenant,
                                                                                        TERMINAL_STATUSES))
                                                        .flatMap(existsActive -> {
                                                                if (Boolean.TRUE.equals(existsActive)) {
                                                                        log.warn("Operational resource already has an active maintenance order");
                                                                        return Mono.error(new DomainException(
                                                                                        "Operational resource already has an active maintenance order"));
                                                                }

                                                                String year = String
                                                                                .valueOf(LocalDateTime.now().getYear());
                                                                String suffix = UUID.randomUUID().toString()
                                                                                .substring(0, 6).toUpperCase();
                                                                String orderCode = "ORD-" + year + "-" + suffix;

                                order.setOrderCode(orderCode);
                                order.setOrderStatus(OrderStatus.REGISTERED);
                                order.setStatus("A");
                                order.setActive(true);
                                order.setCreatedAt(LocalDateTime.now());
                                order.setUpdatedAt(LocalDateTime.now());

                                                                Mono<Void> notifyResource = (token != null
                                                                                && !token.isBlank())
                                                                                                ? resourceClientPort
                                                                                                                .disableResourceForMaintenance(
                                                                                                                                order.getResourceId(),
                                                                                                                                token)
                                                                                                : resourceClientPort
                                                                                                                .disableResourceForMaintenance(
                                                                                                                                order.getResourceId());

                                                                return notifyResource
                                                                                .onErrorResume(err -> {
                                                                                        log.warn("Notice: Could not communicate directly with vg-ms-recursos (upstream unavailable), order will be saved locally. Type: {}",
                                                                                                        err.getClass().getSimpleName());
                                                                                        return Mono.empty();
                                                                                })
                                                                                .then(repositoryPort.save(order))
                                                                                .flatMap(saved -> audit("CREATE",
                                                                                                saved.getOrderCode(),
                                                                                                saved.getResourceId(),
                                                                                                actor,
                                                                                                saved.getEntityId(),
                                                                                                "SUCCESS",
                                                                                                correlationId, null)
                                                                                                .thenReturn(saved));
                                                        });
                                }).onErrorResume(err -> {
                                        if (isAuditPersistenceError(err)) {
                                                return Mono.error(err);
                                        }
                                        return authClientPort.getCurrentEntityId()
                                                        .filter(eid -> !eid.isBlank())
                                                        .defaultIfEmpty("")
                                                        .flatMap(callerEid -> {
                                                                String resolvedTenant = effectiveTenantRef.get() != null
                                                                                ? effectiveTenantRef.get()
                                                                                : (!callerEid.isBlank() ? callerEid
                                                                                                : order.getEntityId());
                                                                return audit("CREATE", order.getOrderCode(),
                                                                                order.getResourceId(),
                                                                                actor, resolvedTenant, "FAILED",
                                                                                correlationId, err)
                                                                                .then(Mono.error(err));
                                                        });
                                }));
        }

        @Override
        public Mono<MaintenanceOrder> update(String id, MaintenanceOrder updatedDetails) {
                log.info("Updating editable header and workshop fields for maintenance order");
                String correlationId = UUID.randomUUID().toString();
                AtomicReference<MaintenanceOrder> loadedOrder = new AtomicReference<>();
                AtomicReference<String> callerTenantRef = new AtomicReference<>();

                return resolveCurrentActor().flatMap(actor -> loadOrderForAuthorizedMutation(id, callerTenantRef)
                                .flatMap(order -> {
                                        loadedOrder.set(order);
                                        return Mono.<MaintenanceOrder>fromCallable(() -> {
                                                order.updateDetails(
                                                                updatedDetails.getFailureReason(),
                                                                updatedDetails.getReportedMileage(),
                                                                updatedDetails.getPriority(),
                                                                updatedDetails.getRemarks(),
                                                                updatedDetails.getIncidentId(),
                                                                updatedDetails.getWorkshopDetail());
                                                return order;
                                        }).flatMap(repositoryPort::save)
                                                        .flatMap(saved -> audit("UPDATE", saved.getOrderCode(),
                                                                        saved.getResourceId(),
                                                                        actor, saved.getEntityId(), "SUCCESS",
                                                                        correlationId, null)
                                                                        .thenReturn(saved));
                                })
                                .onErrorResume(err -> auditFailure("UPDATE", id, loadedOrder.get(),
                                                actor, callerTenantRef.get(), correlationId, err)));
        }

        @Override
        public Mono<MaintenanceOrder> assignWorkshop(String id, WorkshopDetail workshop) {
                String correlationId = UUID.randomUUID().toString();
                AtomicReference<MaintenanceOrder> loadedOrder = new AtomicReference<>();
                AtomicReference<String> callerTenantRef = new AtomicReference<>();

                return resolveCurrentActor().flatMap(actor -> loadOrderForAuthorizedMutation(id, callerTenantRef)
                                .flatMap(order -> {
                                        loadedOrder.set(order);
                                        return Mono.<MaintenanceOrder>fromCallable(() -> {
                                                order.assignWorkshop(workshop);
                                                return order;
                                        }).flatMap(repositoryPort::save)
                                                        .flatMap(saved -> audit("ASSIGN_WORKSHOP", saved.getOrderCode(),
                                                                        saved.getResourceId(),
                                                                        actor, saved.getEntityId(), "SUCCESS",
                                                                        correlationId, null)
                                                                        .thenReturn(saved));
                                })
                                .onErrorResume(err -> auditFailure("ASSIGN_WORKSHOP", id, loadedOrder.get(),
                                                actor, callerTenantRef.get(), correlationId, err)));
        }

        @Override
        public Mono<MaintenanceOrder> startRepair(String id) {
                String correlationId = UUID.randomUUID().toString();
                AtomicReference<MaintenanceOrder> loadedOrder = new AtomicReference<>();
                AtomicReference<String> callerTenantRef = new AtomicReference<>();

                return resolveCurrentActor().flatMap(actor -> loadOrderForAuthorizedMutation(id, callerTenantRef)
                                .flatMap(order -> {
                                        loadedOrder.set(order);
                                        return Mono.<MaintenanceOrder>fromCallable(() -> {
                                                order.startRepair();
                                                return order;
                                        }).flatMap(repositoryPort::save)
                                                        .flatMap(saved -> audit("START_REPAIR", saved.getOrderCode(),
                                                                        saved.getResourceId(),
                                                                        actor, saved.getEntityId(), "SUCCESS",
                                                                        correlationId, null)
                                                                        .thenReturn(saved));
                                })
                                .onErrorResume(err -> auditFailure("START_REPAIR", id, loadedOrder.get(),
                                                actor, callerTenantRef.get(), correlationId, err)));
        }

        @Override
        public Mono<MaintenanceOrder> passToQualityControl(String id) {
                String correlationId = UUID.randomUUID().toString();
                AtomicReference<MaintenanceOrder> loadedOrder = new AtomicReference<>();
                AtomicReference<String> callerTenantRef = new AtomicReference<>();

                return resolveCurrentActor().flatMap(actor -> loadOrderForAuthorizedMutation(id, callerTenantRef)
                                .flatMap(order -> {
                                        loadedOrder.set(order);
                                        return Mono.<MaintenanceOrder>fromCallable(() -> {
                                                order.passToQualityControl();
                                                return order;
                                        }).flatMap(repositoryPort::save)
                                                        .flatMap(saved -> audit("QUALITY_CONTROL", saved.getOrderCode(),
                                                                        saved.getResourceId(),
                                                                        actor, saved.getEntityId(), "SUCCESS",
                                                                        correlationId, null)
                                                                        .thenReturn(saved));
                                })
                                .onErrorResume(err -> auditFailure("QUALITY_CONTROL", id, loadedOrder.get(),
                                                actor, callerTenantRef.get(), correlationId, err)));
        }

        @Override
        public Mono<MaintenanceOrder> issueTechnicalDischarge(String id, TechnicalDischargeReport dischargeReport) {
                return issueTechnicalDischarge(id, dischargeReport, null);
        }

        @Override
        public Mono<MaintenanceOrder> issueTechnicalDischarge(
                        String id,
                        TechnicalDischargeReport dischargeReport,
                        String token) {
                log.info("Issuing technical discharge for maintenance order");
                String correlationId = UUID.randomUUID().toString();
                AtomicReference<MaintenanceOrder> loadedOrder = new AtomicReference<>();
                AtomicReference<String> callerTenantRef = new AtomicReference<>();

                return resolveCurrentActor().flatMap(actor -> loadOrderForAuthorizedMutation(id, callerTenantRef)
                                .flatMap(order -> {
                                        loadedOrder.set(order);
                                        return Mono.fromCallable(() -> {
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
                                                                .flatMap(saved -> audit("TECHNICAL_DISCHARGE",
                                                                                saved.getOrderCode(),
                                                                                saved.getResourceId(), actor,
                                                                                saved.getEntityId(),
                                                                                "SUCCESS", correlationId, null)
                                                                                .thenReturn(saved));
                                        });
                                })
                                .onErrorResume(err -> auditFailure("TECHNICAL_DISCHARGE", id, loadedOrder.get(),
                                                actor, callerTenantRef.get(), correlationId, err)));
        }

        @Override
        public Mono<MaintenanceOrder> cancel(String id, String reason, String remarks) {
                return cancel(id, reason, remarks, null);
        }

        @Override
        public Mono<MaintenanceOrder> cancel(String id, String reason, String remarks, String token) {
                log.info("Canceling maintenance order");
                String correlationId = UUID.randomUUID().toString();
                AtomicReference<MaintenanceOrder> loadedOrder = new AtomicReference<>();
                AtomicReference<String> callerTenantRef = new AtomicReference<>();

                return resolveCurrentActor().flatMap(actor -> loadOrderForAuthorizedMutation(id, callerTenantRef)
                                .flatMap(order -> {
                                        loadedOrder.set(order);
                                        return Mono.fromCallable(() -> {
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
                                                                .flatMap(saved -> audit("CANCEL", saved.getOrderCode(),
                                                                                saved.getResourceId(), actor,
                                                                                saved.getEntityId(),
                                                                                "SUCCESS", correlationId, null)
                                                                                .thenReturn(saved));
                                        });
                                })
                                .onErrorResume(err -> auditFailure("CANCEL", id, loadedOrder.get(),
                                                actor, callerTenantRef.get(), correlationId, err)));
        }

        private Mono<MaintenanceOrder> loadOrderForAuthorizedMutation(
                        String id,
                        AtomicReference<String> callerTenantRef) {
                return authClientPort.hasGlobalAccess()
                                .defaultIfEmpty(false)
                                .flatMap(isGlobal -> {
                                        if (Boolean.TRUE.equals(isGlobal)) {
                                                return repositoryPort.findById(id)
                                                                .switchIfEmpty(Mono.error(
                                                                                DomainException.notFound(
                                                                                                "Requested maintenance order was not found")))
                                                                .flatMap(order -> {
                                                                        if (order.getEntityId() == null || order
                                                                                        .getEntityId().isBlank()) {
                                                                                return Mono.error(new DomainException(
                                                                                                "Persisted maintenance order lacks an owning entity"));
                                                                        }
                                                                        callerTenantRef.set(order.getEntityId());
                                                                        return Mono.just(order);
                                                                });
                                        }
                                        return requireAuthenticatedEntityId()
                                                        .flatMap(callerEntityId -> {
                                                                callerTenantRef.set(callerEntityId);
                                                                return repositoryPort
                                                                                .findByIdAndEntityId(id, callerEntityId)
                                                                                .switchIfEmpty(Mono.error(
                                                                                                DomainException.notFound(
                                                                                                                "Requested maintenance order was not found")));
                                                        });
                                });
        }

        private Mono<String> requireAuthenticatedEntityId() {
                return authClientPort.getCurrentEntityId()
                                .filter(eid -> !eid.isBlank())
                                .switchIfEmpty(Mono.error(new AccessDeniedException(
                                                "Access denied: authenticated tenant claim is required")));
        }

        private Mono<MaintenanceOrder> auditFailure(
                        String action,
                        String orderId,
                        MaintenanceOrder loadedOrder,
                        String actor,
                        String callerTenant,
                        String correlationId,
                        Throwable error) {
                if (isAuditPersistenceError(error)) {
                        return Mono.error(error);
                }
                String orderRef = (loadedOrder != null && loadedOrder.getOrderCode() != null)
                                ? loadedOrder.getOrderCode()
                                : orderId;
                String resourceRef = (loadedOrder != null && loadedOrder.getResourceId() != null)
                                ? loadedOrder.getResourceId()
                                : orderId;
                String tenantRef = (loadedOrder != null && loadedOrder.getEntityId() != null)
                                ? loadedOrder.getEntityId()
                                : callerTenant;

                return audit(action, orderRef, resourceRef, actor, tenantRef, "FAILED", correlationId, error)
                                .then(Mono.error(error));
        }
        String orderRef = (loadedOrder != null && loadedOrder.getOrderCode() != null)
                ? loadedOrder.getOrderCode()
                : orderId;
        String resourceRef = (loadedOrder != null && loadedOrder.getResourceId() != null)
                ? loadedOrder.getResourceId()
                : "RESOURCE_UNAVAILABLE";
        String tenantRef = (loadedOrder != null && loadedOrder.getEntityId() != null)
                ? loadedOrder.getEntityId()
                : callerTenant;

        private boolean isAuditPersistenceError(Throwable error) {
                return error instanceof DomainException
                                && AUDIT_PERSISTENCE_FAILURE_MESSAGE.equals(error.getMessage());
        }

        private Mono<Void> audit(
                        String action,
                        String orderCode,
                        String resourceId,
                        String actor,
                        String tenant,
                        String result,
                        String correlationId,
                        Throwable error) {
                RecordAuditLogRequest auditRequest = AuditLogMapper.toRequest(
                                action, orderCode, resourceId, actor, tenant, result, correlationId, error);
                AuditLogResponse auditEntry = AuditLogMapper.toResponse(auditRequest);

                log.info("AUDIT [timestamp={}, action={}, result={}, correlationId={}, error={}]",
                                auditEntry.timestamp(),
                                auditEntry.action(),
                                auditEntry.result(),
                                auditEntry.correlationId(),
                                auditEntry.errorType());

                return Objects.requireNonNull(auditLogRepositoryPort, "AuditLogRepositoryPort is required")
                                .recordAudit(
                                                auditEntry.action(),
                                                auditEntry.orderCode(),
                                                auditEntry.resourceId(),
                                                auditEntry.actor(),
                                                auditEntry.tenant(),
                                                auditEntry.result(),
                                                auditEntry.correlationId(),
                                                auditEntry.errorType())
                                .onErrorMap(auditErr -> {
                                        log.error("Failed to persist mandatory durable audit log [action={}, result={}, correlationId={}, type={}]",
                                                        auditEntry.action(),
                                                        auditEntry.result(),
                                                        auditEntry.correlationId(),
                                                        auditErr.getClass().getSimpleName());
                                        return new DomainException(AUDIT_PERSISTENCE_FAILURE_MESSAGE);
                                });
        }

        private Mono<String> resolveCurrentActor() {
                return authClientPort.getCurrentUsername()
                                .defaultIfEmpty("ANONYMOUS");
        }

        private Mono<String> resolveEffectiveEntityId(String requestedEntityId) {
                return authClientPort.hasGlobalAccess()
                                .defaultIfEmpty(false)
                                .flatMap(isGlobal -> authClientPort.getCurrentEntityId()
                                                .defaultIfEmpty("")
                                                .flatMap(callerEntityId -> {
                                                        String normalizedCaller = callerEntityId.isBlank() ? null
                                                                        : callerEntityId;
                                                        String normalizedRequested = (requestedEntityId != null
                                                                        && !requestedEntityId.isBlank())
                                                                                        ? requestedEntityId
                                                                                        : null;

                                                        if (Boolean.TRUE.equals(isGlobal)) {
                                                                String effective = normalizedRequested != null
                                                                                ? normalizedRequested
                                                                                : normalizedCaller;
                                                                if (effective == null) {
                                                                        return Mono.error(new DomainException(
                                                                                        "Entity ID is required to create a maintenance order"));
                                                                }
                                                                return Mono.just(effective);
                                                        }

                                                        if (normalizedCaller == null) {
                                                                return Mono.error(new AccessDeniedException(
                                                                                "Access denied: Fleet vehicles are only managed by the owning entity"));
                                                        }
                                                        if (normalizedRequested != null && !normalizedRequested
                                                                        .equals(normalizedCaller)) {
                                                                return Mono.error(new AccessDeniedException(
                                                                                "Access denied: Fleet vehicles are only managed by the owning entity"));
                                                        }
                                                        return Mono.just(normalizedCaller);
                                                }));
        }
}
