package vg_ms.flota.orden.domain.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vg_ms.flota.orden.domain.aggregate.OrdenMantenimiento;
import vg_ms.flota.orden.domain.vo.EstadoOrden;

public interface OrdenMantenimientoRepository {

    Mono<OrdenMantenimiento> save(OrdenMantenimiento orden);

    Mono<OrdenMantenimiento> findById(String id);

    Mono<OrdenMantenimiento> findByCodigoOrden(String codigoOrden);

    Flux<OrdenMantenimiento> findAll();

    Flux<OrdenMantenimiento> findByRecursoId(String recursoId);

    Flux<OrdenMantenimiento> findByEntidadId(String entidadId);

    Flux<OrdenMantenimiento> findByEstadoOrden(EstadoOrden estadoOrden);

    Mono<Boolean> existsByRecursoIdAndEstadoOrdenNotIn(String recursoId, java.util.List<EstadoOrden> estadosFinales);
}
