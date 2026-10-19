package pe.edu.vallegrande.flota.application.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.flota.application.services.OrdenMantenimientoService;
import pe.edu.vallegrande.flota.domain.enums.EstadoOrden;
import pe.edu.vallegrande.flota.domain.exception.DomainException;
import pe.edu.vallegrande.flota.domain.models.DetalleTaller;
import pe.edu.vallegrande.flota.domain.models.InformeAltaTecnica;
import pe.edu.vallegrande.flota.domain.models.OrdenMantenimiento;
import pe.edu.vallegrande.flota.infrastructure.client.external.RecursoOperativoClient;
import pe.edu.vallegrande.flota.infrastructure.dto.request.AltaOperativaRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.request.AsignarTallerRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.request.CancelarOrdenRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.request.CreateOrdenMantenimientoRequest;
import pe.edu.vallegrande.flota.infrastructure.mapper.OrdenMantenimientoMapper;
import pe.edu.vallegrande.flota.infrastructure.repository.OrdenMantenimientoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementación del servicio de aplicación de Órdenes de Mantenimiento.
 * Orquesta transaccionalmente la persistencia en MongoDB y las llamadas reactivas a vg-ms-recursos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdenMantenimientoServiceImpl implements OrdenMantenimientoService {

    private final OrdenMantenimientoRepository repository;
    private final RecursoOperativoClient recursoClient;

    private static final List<String> ESTADOS_FINALES = List.of(
            EstadoOrden.FINALIZADA.name(),
            EstadoOrden.CANCELADA.name()
    );

    @Override
    public Flux<OrdenMantenimiento> findAll() {
        return repository.findAll()
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Mono<OrdenMantenimiento> findById(String id) {
        return repository.findById(id)
                .map(OrdenMantenimientoMapper::toDomain)
                .switchIfEmpty(Mono.error(new DomainException("Orden de mantenimiento no encontrada con ID: " + id)));
    }

    @Override
    public Mono<OrdenMantenimiento> findByCodigo(String codigo) {
        return repository.findByCodigoOrden(codigo)
                .map(OrdenMantenimientoMapper::toDomain)
                .switchIfEmpty(Mono.error(new DomainException("Orden de mantenimiento no encontrada con código: " + codigo)));
    }

    @Override
    public Flux<OrdenMantenimiento> findByRecurso(String recursoId) {
        return repository.findByRecursoId(recursoId)
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Flux<OrdenMantenimiento> findByEntidad(String entidadId) {
        return repository.findByEntidadId(entidadId)
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Flux<OrdenMantenimiento> findByEstado(EstadoOrden estado) {
        return repository.findByEstadoOrden(estado.name())
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Mono<OrdenMantenimiento> create(CreateOrdenMantenimientoRequest request, String token, String reportadoPorDni) {
        log.info("Iniciando creación de orden para recurso: {}", request.getRecursoId());

        return repository.existsByRecursoIdAndEstadoOrdenNotIn(request.getRecursoId(), ESTADOS_FINALES)
                .flatMap(existeActiva -> {
                    if (Boolean.TRUE.equals(existeActiva)) {
                        return Mono.error(new DomainException(
                                "El recurso operativo " + request.getRecursoId() + " ya tiene una orden de mantenimiento activa"
                        ));
                    }

                    String anio = String.valueOf(LocalDateTime.now().getYear());
                    String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                    String codigoOrden = "ORD-" + anio + "-" + sufijo;

                    OrdenMantenimiento nuevaOrden = OrdenMantenimientoMapper.toDomain(request, codigoOrden, reportadoPorDni);

                    // Orquestación: inhabilitar recurso a EN_MANTENIMIENTO en vg-ms-recursos y guardar orden
                    return recursoClient.inhabilitarRecursoPorMantenimiento(request.getRecursoId(), token)
                            .onErrorResume(err -> {
                                log.warn("Aviso: No se pudo contactar directamente con vg-ms-recursos ({}), se creará orden localmente.", err.getMessage());
                                return Mono.empty();
                            })
                            .then(repository.save(OrdenMantenimientoMapper.toDocument(nuevaOrden)))
                            .map(OrdenMantenimientoMapper::toDomain);
                });
    }

    @Override
    public Mono<OrdenMantenimiento> asignarTaller(String id, AsignarTallerRequest request) {
        return findById(id)
                .flatMap(orden -> {
                    DetalleTaller taller = DetalleTaller.builder()
                            .nombreTaller(request.getNombreTaller())
                            .rucTaller(request.getRucTaller())
                            .mecanicoResponsable(request.getMecanicoResponsable())
                            .telefonoContacto(request.getTelefonoContacto())
                            .costoEstimado(request.getCostoEstimado())
                            .fechaEstimadaEntrega(request.getFechaEstimadaEntrega())
                            .diagnosticoInicial(request.getDiagnosticoInicial())
                            .build();

                    orden.asignarTaller(taller);
                    return repository.save(OrdenMantenimientoMapper.toDocument(orden));
                })
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Mono<OrdenMantenimiento> iniciarReparacion(String id) {
        return findById(id)
                .flatMap(orden -> {
                    orden.iniciarReparacion();
                    return repository.save(OrdenMantenimientoMapper.toDocument(orden));
                })
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Mono<OrdenMantenimiento> pasarAControlCalidad(String id) {
        return findById(id)
                .flatMap(orden -> {
                    orden.pasarAControlCalidad();
                    return repository.save(OrdenMantenimientoMapper.toDocument(orden));
                })
                .map(OrdenMantenimientoMapper::toDomain);
    }

    @Override
    public Mono<OrdenMantenimiento> emitirAltaOperativa(String id, AltaOperativaRequest request, String token, String aprobadoPorDni) {
        log.info("Iniciando emisión de alta técnica para orden: {}", id);

        return findById(id)
                .flatMap(orden -> {
                    InformeAltaTecnica informe = InformeAltaTecnica.builder()
                            .dictamenTecnico(request.getDictamenTecnico())
                            .pruebasRealizadas(request.getPruebasRealizadas())
                            .kilometrajeSalida(request.getKilometrajeSalida())
                            .fechaAlta(LocalDateTime.now())
                            .aprobadoPorDni(aprobadoPorDni)
                            .observaciones(request.getObservaciones())
                            .build();

                    if (request.getCostoFinal() != null && orden.getDetalleTaller() != null) {
                        DetalleTaller tallerActualizado = DetalleTaller.builder()
                                .nombreTaller(orden.getDetalleTaller().getNombreTaller())
                                .rucTaller(orden.getDetalleTaller().getRucTaller())
                                .mecanicoResponsable(orden.getDetalleTaller().getMecanicoResponsable())
                                .telefonoContacto(orden.getDetalleTaller().getTelefonoContacto())
                                .costoEstimado(orden.getDetalleTaller().getCostoEstimado())
                                .costoFinal(request.getCostoFinal())
                                .fechaEstimadaEntrega(orden.getDetalleTaller().getFechaEstimadaEntrega())
                                .diagnosticoInicial(orden.getDetalleTaller().getDiagnosticoInicial())
                                .build();
                        orden.asignarTaller(tallerActualizado);
                    }

                    orden.emitirAltaOperativa(informe);

                    // Orquestación: rehabilitar recurso a DISPONIBLE en vg-ms-recursos y guardar orden
                    return recursoClient.habilitarRecursoPorAltaOperativa(orden.getRecursoId(), token)
                            .onErrorResume(err -> {
                                log.warn("Aviso: No se pudo actualizar estado en vg-ms-recursos ({}). Se finaliza orden localmente.", err.getMessage());
                                return Mono.empty();
                            })
                            .then(repository.save(OrdenMantenimientoMapper.toDocument(orden)))
                            .map(OrdenMantenimientoMapper::toDomain);
                });
    }

    @Override
    public Mono<OrdenMantenimiento> cancelar(String id, CancelarOrdenRequest request, String token) {
        return findById(id)
                .flatMap(orden -> {
                    orden.cancelar(request.getMotivo());

                    // Rehabilitar recurso en vg-ms-recursos
                    return recursoClient.habilitarRecursoPorAltaOperativa(orden.getRecursoId(), token)
                            .onErrorResume(err -> {
                                log.warn("Aviso: No se pudo rehabilitar recurso en vg-ms-recursos al cancelar: {}", err.getMessage());
                                return Mono.empty();
                            })
                            .then(repository.save(OrdenMantenimientoMapper.toDocument(orden)))
                            .map(OrdenMantenimientoMapper::toDomain);
                });
    }
}
