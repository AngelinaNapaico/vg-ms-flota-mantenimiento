package vg_ms.flota.orden.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vg_ms.flota.orden.domain.aggregate.OrdenMantenimiento;
import vg_ms.flota.orden.domain.repository.OrdenMantenimientoRepository;
import vg_ms.flota.orden.domain.vo.EstadoOrden;

import java.util.List;

@Repository
public class MongoOrdenMantenimientoRepository implements OrdenMantenimientoRepository {

    private final SpringDataOrdenMantenimientoRepository springRepo;

    public MongoOrdenMantenimientoRepository(SpringDataOrdenMantenimientoRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Mono<OrdenMantenimiento> save(OrdenMantenimiento orden) {
        return springRepo.save(OrdenMantenimientoMapper.toDocument(orden))
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Mono<OrdenMantenimiento> findById(String id) {
        return springRepo.findById(id)
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Mono<OrdenMantenimiento> findByCodigoOrden(String codigoOrden) {
        return springRepo.findByCodigoOrden(codigoOrden)
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Flux<OrdenMantenimiento> findAll() {
        return springRepo.findAll()
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Flux<OrdenMantenimiento> findByRecursoId(String recursoId) {
        return springRepo.findByRecursoId(recursoId)
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Flux<OrdenMantenimiento> findByEntidadId(String entidadId) {
        return springRepo.findByEntidadId(entidadId)
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Flux<OrdenMantenimiento> findByEstadoOrden(EstadoOrden estadoOrden) {
        return springRepo.findByEstadoOrden(estadoOrden.name())
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByRecursoIdAndEstadoOrdenNotIn(String recursoId, List<EstadoOrden> estadosFinales) {
        List<String> estadosStr = estadosFinales.stream().map(Enum::name).toList();
        return springRepo.existsByRecursoIdAndEstadoOrdenNotIn(recursoId, estadosStr);
    }
}
