package vg_ms.flota.orden.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vg_ms.flota.orden.application.port.RecursoOperativoPort;
import vg_ms.flota.orden.domain.aggregate.OrdenMantenimiento;
import vg_ms.flota.orden.domain.repository.OrdenMantenimientoRepository;
import vg_ms.flota.orden.domain.vo.*;
import vg_ms.flota.orden.interfaces.dto.*;
import vg_ms.flota.shared.exception.DomainException;
import vg_ms.flota.shared.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdenMantenimientoServiceImpl implements OrdenMantenimientoService {

    private final OrdenMantenimientoRepository ordenRepository;
    private final RecursoOperativoPort recursoOperativoPort;

    private static final List<EstadoOrden> ESTADOS_FINALES = List.of(EstadoOrden.FINALIZADA, EstadoOrden.CANCELADA);

    /**
     * Caso de uso transaccional: Apertura de orden de mantenimiento e inhabilitación del vehículo
     */
    @Override
    public Mono<OrdenMantenimientoResponse> crearOrden(CrearOrdenRequest request, String token, String reportadoPorDni) {
        log.info("Iniciando creación de orden de mantenimiento para recurso: {}", request.getRecursoId());

        return ordenRepository.existsByRecursoIdAndEstadoOrdenNotIn(request.getRecursoId(), ESTADOS_FINALES)
                .flatMap(existeActiva -> {
                    if (Boolean.TRUE.equals(existeActiva)) {
                        return Mono.error(new DomainException(
                                "El recurso operativo " + request.getRecursoId() + " ya tiene una orden de mantenimiento activa"
                        ));
                    }

                    String anio = String.valueOf(LocalDateTime.now().getYear());
                    String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                    String codigoOrden = "ORD-" + anio + "-" + sufijo;

                    OrdenMantenimiento nuevaOrden = OrdenMantenimiento.crear(
                            codigoOrden,
                            request.getRecursoId(),
                            request.getRecursoPlaca(),
                            request.getEntidadId(),
                            reportadoPorDni,
                            request.getIncidenteId(),
                            request.getTipoMantenimiento(),
                            request.getPrioridad(),
                            request.getMotivoAveria(),
                            request.getKilometrajeReportado(),
                            request.getObservaciones()
                    );

                    // Transacción coordinada: Cambiar recurso a EN_MANTENIMIENTO en vg-ms-recursos y guardar orden
                    return recursoOperativoPort.inhabilitarRecursoPorMantenimiento(request.getRecursoId(), token)
                            .onErrorResume(err -> {
                                log.warn("Aviso: No se pudo contactar directamente con vg-ms-recursos ({}), se creará la orden localmente.", err.getMessage());
                                return Mono.empty();
                            })
                            .then(ordenRepository.save(nuevaOrden))
                            .map(OrdenMantenimientoResponse::fromDomain);
                });
    }

    /**
     * Asignación de taller mecánico y presupuesto inicial
     */
    @Override
    public Mono<OrdenMantenimientoResponse> asignarTaller(String id, AsignarTallerRequest request) {
        return findDomainById(id)
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
                    return ordenRepository.save(orden);
                })
                .map(OrdenMantenimientoResponse::fromDomain);
    }

    /**
     * Pasa la orden al estado EN_TALLER
     */
    @Override
    public Mono<OrdenMantenimientoResponse> iniciarReparacion(String id) {
        return findDomainById(id)
                .flatMap(orden -> {
                    orden.iniciarReparacion();
                    return ordenRepository.save(orden);
                })
                .map(OrdenMantenimientoResponse::fromDomain);
    }

    /**
     * Pasa la orden al estado CONTROL_CALIDAD
     */
    @Override
    public Mono<OrdenMantenimientoResponse> pasarAControlCalidad(String id) {
        return findDomainById(id)
                .flatMap(orden -> {
                    orden.pasarAControlCalidad();
                    return ordenRepository.save(orden);
                })
                .map(OrdenMantenimientoResponse::fromDomain);
    }

    /**
     * Caso de uso transaccional: Emisión de alta técnica, finalización de orden y retorno a DISPONIBLE en vg-ms-recursos
     */
    @Override
    public Mono<OrdenMantenimientoResponse> emitirAltaOperativa(
            String id,
            AltaOperativaRequest request,
            String token,
            String aprobadoPorDni) {

        log.info("Iniciando emisión de alta técnica para orden: {}", id);

        return findDomainById(id)
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

                    // Transacción de retorno: Habilitar de nuevo el recurso a DISPONIBLE en vg-ms-recursos
                    return recursoOperativoPort.habilitarRecursoPorAltaOperativa(orden.getRecursoId(), token)
                            .onErrorResume(err -> {
                                log.warn("Aviso: No se pudo actualizar estado en vg-ms-recursos ({}). Se finaliza orden localmente.", err.getMessage());
                                return Mono.empty();
                            })
                            .then(ordenRepository.save(orden))
                            .map(OrdenMantenimientoResponse::fromDomain);
                });
    }

    /**
     * Cancelar orden y rehabilitar el recurso a DISPONIBLE
     */
    @Override
    public Mono<OrdenMantenimientoResponse> cancelarOrden(String id, CancelarOrdenRequest request, String token) {
        return findDomainById(id)
                .flatMap(orden -> {
                    orden.cancelar(request.getMotivo());

                    return recursoOperativoPort.habilitarRecursoPorAltaOperativa(orden.getRecursoId(), token)
                            .onErrorResume(e -> Mono.empty())
                            .then(ordenRepository.save(orden))
                            .map(OrdenMantenimientoResponse::fromDomain);
                });
    }

    // ─── Consultas ──────────────────────────────────────────────────────────

    @Override
    public Mono<OrdenMantenimientoResponse> obtenerPorId(String id) {
        return findDomainById(id).map(OrdenMantenimientoResponse::fromDomain);
    }

    @Override
    public Mono<OrdenMantenimientoResponse> obtenerPorCodigo(String codigoOrden) {
        return ordenRepository.findByCodigoOrden(codigoOrden)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Orden con código " + codigoOrden + " no encontrada")))
                .map(OrdenMantenimientoResponse::fromDomain);
    }

    @Override
    public Flux<OrdenMantenimientoResponse> listarTodas() {
        return ordenRepository.findAll().map(OrdenMantenimientoResponse::fromDomain);
    }

    @Override
    public Flux<OrdenMantenimientoResponse> listarPorRecurso(String recursoId) {
        return ordenRepository.findByRecursoId(recursoId).map(OrdenMantenimientoResponse::fromDomain);
    }

    @Override
    public Flux<OrdenMantenimientoResponse> listarPorEntidad(String entidadId) {
        return ordenRepository.findByEntidadId(entidadId).map(OrdenMantenimientoResponse::fromDomain);
    }

    @Override
    public Flux<OrdenMantenimientoResponse> listarPorEstado(EstadoOrden estado) {
        return ordenRepository.findByEstadoOrden(estado).map(OrdenMantenimientoResponse::fromDomain);
    }

    private Mono<OrdenMantenimiento> findDomainById(String id) {
        return ordenRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Orden de mantenimiento no encontrada con ID: " + id)));
    }
}
