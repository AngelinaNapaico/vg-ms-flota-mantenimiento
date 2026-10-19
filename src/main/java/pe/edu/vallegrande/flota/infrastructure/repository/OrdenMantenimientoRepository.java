package pe.edu.vallegrande.flota.infrastructure.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.flota.infrastructure.document.BaseDocument;
import pe.edu.vallegrande.flota.infrastructure.document.OrdenMantenimientoDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Repositorio reactivo de MongoDB para órdenes de mantenimiento según estándar PR241.
 */
@Repository
public interface OrdenMantenimientoRepository extends ReactiveMongoRepository<OrdenMantenimientoDocument, String> {

    Mono<OrdenMantenimientoDocument> findByCodigoOrden(String codigoOrden);

    Flux<OrdenMantenimientoDocument> findByRecursoId(String recursoId);

    Flux<OrdenMantenimientoDocument> findByEntidadId(String entidadId);

    Flux<OrdenMantenimientoDocument> findByEstadoOrden(String estadoOrden);

    Flux<OrdenMantenimientoDocument> findByStatus(BaseDocument.Status status);

    Mono<Boolean> existsByRecursoIdAndEstadoOrdenNotIn(String recursoId, Collection<String> estados);

    Flux<OrdenMantenimientoDocument> findByRecursoPlaca(String recursoPlaca);
}
