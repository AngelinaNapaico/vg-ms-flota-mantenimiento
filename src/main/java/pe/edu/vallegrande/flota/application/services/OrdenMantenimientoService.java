package pe.edu.vallegrande.flota.application.services;

import pe.edu.vallegrande.flota.domain.enums.EstadoOrden;
import pe.edu.vallegrande.flota.domain.models.OrdenMantenimiento;
import pe.edu.vallegrande.flota.infrastructure.dto.request.AltaOperativaRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.request.AsignarTallerRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.request.CancelarOrdenRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.request.CreateOrdenMantenimientoRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Contrato de servicio de aplicación para órdenes de mantenimiento según estándar PR241.
 */
public interface OrdenMantenimientoService {

    Flux<OrdenMantenimiento> findAll();

    Mono<OrdenMantenimiento> findById(String id);

    Mono<OrdenMantenimiento> findByCodigo(String codigo);

    Flux<OrdenMantenimiento> findByRecurso(String recursoId);

    Flux<OrdenMantenimiento> findByEntidad(String entidadId);

    Flux<OrdenMantenimiento> findByEstado(EstadoOrden estado);

    Mono<OrdenMantenimiento> create(CreateOrdenMantenimientoRequest request, String token, String reportadoPorDni);

    Mono<OrdenMantenimiento> asignarTaller(String id, AsignarTallerRequest request);

    Mono<OrdenMantenimiento> iniciarReparacion(String id);

    Mono<OrdenMantenimiento> pasarAControlCalidad(String id);

    Mono<OrdenMantenimiento> emitirAltaOperativa(String id, AltaOperativaRequest request, String token, String aprobadoPorDni);

    Mono<OrdenMantenimiento> cancelar(String id, CancelarOrdenRequest request, String token);
}
