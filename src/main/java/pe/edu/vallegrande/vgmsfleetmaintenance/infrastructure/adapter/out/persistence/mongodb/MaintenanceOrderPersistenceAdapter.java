package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.MaintenanceOrder;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.MaintenanceOrderRepositoryPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.MaintenanceOrderDocument;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.mapper.MaintenanceOrderDocumentMapper;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.MaintenanceOrderRepository;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security.TenantScope;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Secondary Outbound Persistence Adapter for MongoDB with multi-tenancy.
 * Implements MaintenanceOrderRepositoryPort isolating domain from database details.
 * REC-02/03 & TEN-01/02 & STD-086 compliant:
 * - SUPER_ADMIN/ADMIN has global view and management.
 * - MUNICIPALIDAD_ADMIN has supervisory read-only access.
 * - Entity users access and manage only their own entity's maintenance orders.
 */
@Component
@RequiredArgsConstructor
public class MaintenanceOrderPersistenceAdapter implements MaintenanceOrderRepositoryPort {

    private final MaintenanceOrderRepository repository;

    @Override
    public Mono<MaintenanceOrder> findById(String id) {
        return TenantScope.current()
                .flatMap(scope -> repository.findById(id)
                        .filter(doc -> canRead(scope, doc))
                        .map(MaintenanceOrderDocumentMapper::toDomain)
                );
    }

    @Override
    public Mono<MaintenanceOrder> findByOrderCode(String orderCode) {
        return TenantScope.current()
                .flatMap(scope -> repository.findByOrderCode(orderCode)
                        .filter(doc -> canRead(scope, doc))
                        .map(MaintenanceOrderDocumentMapper::toDomain)
                );
    }

    @Override
    public Flux<MaintenanceOrder> findByResourceId(String resourceId) {
        return TenantScope.current()
                .flatMapMany(scope -> repository.findByResourceId(resourceId)
                        .filter(doc -> canRead(scope, doc))
                        .map(MaintenanceOrderDocumentMapper::toDomain)
                );
    }

    @Override
    public Flux<MaintenanceOrder> findByEntityId(String entityId) {
        return TenantScope.current()
                .flatMapMany(scope -> {
                    if (scope.all() || scope.hasRole("MUNICIPALIDAD_ADMIN")) {
                        return repository.findByEntityId(entityId);
                    }
                    if (scope.entityId() != null && scope.entityId().equals(entityId)) {
                        return repository.findByEntityId(entityId);
                    }
                    return Flux.empty();
                })
                .map(MaintenanceOrderDocumentMapper::toDomain);
    }

    @Override
    public Flux<MaintenanceOrder> findByOrderStatus(String orderStatus) {
        return TenantScope.current()
                .flatMapMany(scope -> {
                    if (scope.all() || scope.hasRole("MUNICIPALIDAD_ADMIN")) {
                        return repository.findByOrderStatus(orderStatus);
                    }
                    if (scope.entityId() != null) {
                        return repository.findByEntityIdAndOrderStatus(scope.entityId(), orderStatus);
                    }
                    return Flux.empty();
                })
                .map(MaintenanceOrderDocumentMapper::toDomain);
    }

    @Override
    public Flux<MaintenanceOrder> findAll() {
        return TenantScope.current()
                .flatMapMany(scope -> {
                    if (scope.all() || scope.hasRole("MUNICIPALIDAD_ADMIN")) {
                        return repository.findAll();
                    }
                    if (scope.entityId() != null) {
                        return repository.findByEntityId(scope.entityId());
                    }
                    return Flux.empty();
                })
                .map(MaintenanceOrderDocumentMapper::toDomain);
    }

    @Override
    public Mono<MaintenanceOrder> save(MaintenanceOrder order) {
        return TenantScope.current()
                .flatMap(scope -> {
                    MaintenanceOrderDocument document = MaintenanceOrderDocumentMapper.toDocument(order);

                    if (order.getId() != null) {
                        if (!canWrite(scope, document)) {
                            return Mono.error(TenantScope.forbidden());
                        }
                    } else {
                        if (document.getEntityId() == null && scope.entityId() != null) {
                            document.setEntityId(scope.entityId());
                        }
                    }

                    if (document.getCreatedAt() == null) {
                        document.setCreatedAt(LocalDateTime.now());
                    }
                    document.setUpdatedAt(LocalDateTime.now());

                    return repository.save(document)
                            .map(MaintenanceOrderDocumentMapper::toDomain);
                });
    }

    @Override
    public Mono<Boolean> existsByResourceIdAndOrderStatusNotIn(String resourceId, Collection<String> statuses) {
        return repository.existsByResourceIdAndOrderStatusNotIn(resourceId, statuses);
    }

    private boolean canRead(TenantScope scope, MaintenanceOrderDocument doc) {
        return scope.canRead(doc.getEntityId());
    }

    private boolean canWrite(TenantScope scope, MaintenanceOrderDocument doc) {
        return scope.canWrite() && canRead(scope, doc);
    }
}
