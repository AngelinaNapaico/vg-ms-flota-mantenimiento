package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
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
 * Outbound persistence adapter for MongoDB.
 * Implements MaintenanceOrderRepositoryPort with tenant isolation,
 * optimistic locking, and domain-to-document mapping.
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
                        .map(MaintenanceOrderDocumentMapper::toDomain));
    }

    @Override
    public Mono<MaintenanceOrder> findByOrderCode(String orderCode) {
        return TenantScope.current()
                .flatMap(scope -> repository.findByOrderCode(orderCode)
                        .filter(doc -> canRead(scope, doc))
                        .map(MaintenanceOrderDocumentMapper::toDomain));
    }

    @Override
    public Flux<MaintenanceOrder> findByResourceId(String resourceId) {
        return TenantScope.current()
                .flatMapMany(scope -> repository.findByResourceId(resourceId)
                        .filter(doc -> canRead(scope, doc))
                        .map(MaintenanceOrderDocumentMapper::toDomain));
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
                        if (!scope.canWrite()) {
                            return Mono.error(TenantScope.forbidden());
                        }
                        if (!scope.all()) {
                            if (scope.entityId() == null) {
                                return Mono.error(TenantScope.forbidden());
                            }
                            if (document.getEntityId() != null && !document.getEntityId().equals(scope.entityId())) {
                                return Mono.error(TenantScope.forbidden());
                            }
                            document.setEntityId(scope.entityId());
                        }
                    }

                    if (document.getCreatedAt() == null) {
                        document.setCreatedAt(LocalDateTime.now());
                    }
                    document.setUpdatedAt(LocalDateTime.now());

                    return repository.save(document)
                            .onErrorMap(DuplicateKeyException.class,
                                    ex -> new DomainException(
                                            "A maintenance order with this code or active status already exists", ex))
                            .onErrorMap(OptimisticLockingFailureException.class,
                                    ex -> new DomainException(
                                            "The maintenance order was updated concurrently. Please retry.", ex))
                            .map(MaintenanceOrderDocumentMapper::toDomain);
                });
    }

    @Override
    public Mono<Boolean> existsByResourceIdAndOrderStatusNotIn(String resourceId, Collection<String> statuses) {
        return TenantScope.current()
                .flatMap(scope -> {
                    if (scope.all() || scope.entityId() == null) {
                        return repository.existsByResourceIdAndOrderStatusNotIn(resourceId, statuses);
                    }
                    return repository.existsByResourceIdAndEntityIdAndOrderStatusNotIn(resourceId, scope.entityId(),
                            statuses);
                });
    }

    private boolean canRead(TenantScope scope, MaintenanceOrderDocument doc) {
        return scope.canRead(doc.getEntityId());
    }

    private boolean canWrite(TenantScope scope, MaintenanceOrderDocument doc) {
        return scope.canWrite() && canRead(scope, doc);
    }
}
