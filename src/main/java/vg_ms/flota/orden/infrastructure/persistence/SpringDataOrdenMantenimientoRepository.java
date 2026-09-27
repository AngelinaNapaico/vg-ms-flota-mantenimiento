package vg_ms.flota.orden.infrastructure.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface SpringDataOrdenMantenimientoRepository extends ReactiveMongoRepository<OrdenMantenimientoDocument, String> {

    Mono<OrdenMantenimientoDocument> findByCodigoOrden(String codigoOrden);

    Flux<OrdenMantenimientoDocument> findByRecursoId(String recursoId);

    Flux<OrdenMantenimientoDocument> findByEntidadId(String entidadId);

    Flux<OrdenMantenimientoDocument> findByEstadoOrden(String estadoOrden);

    Mono<Boolean> existsByRecursoIdAndEstadoOrdenNotIn(String recursoId, Collection<String> estados);
}
