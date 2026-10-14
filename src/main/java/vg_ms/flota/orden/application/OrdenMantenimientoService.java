package vg_ms.flota.orden.application;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vg_ms.flota.orden.domain.vo.EstadoOrden;
import vg_ms.flota.orden.interfaces.dto.AltaOperativaRequest;
import vg_ms.flota.orden.interfaces.dto.AsignarTallerRequest;
import vg_ms.flota.orden.interfaces.dto.CancelarOrdenRequest;
import vg_ms.flota.orden.interfaces.dto.CrearOrdenRequest;
import vg_ms.flota.orden.interfaces.dto.OrdenMantenimientoResponse;

/**
 * Puerto de entrada de los casos de uso de órdenes de mantenimiento.
 */
public interface OrdenMantenimientoService {

    Mono<OrdenMantenimientoResponse> crearOrden(CrearOrdenRequest request, String token, String reportadoPorDni);

    Mono<OrdenMantenimientoResponse> asignarTaller(String id, AsignarTallerRequest request);

    Mono<OrdenMantenimientoResponse> iniciarReparacion(String id);

    Mono<OrdenMantenimientoResponse> pasarAControlCalidad(String id);

    Mono<OrdenMantenimientoResponse> emitirAltaOperativa(
            String id,
            AltaOperativaRequest request,
            String token,
            String aprobadoPorDni);

    Mono<OrdenMantenimientoResponse> cancelarOrden(String id, CancelarOrdenRequest request, String token);

    Mono<OrdenMantenimientoResponse> obtenerPorId(String id);

    Mono<OrdenMantenimientoResponse> obtenerPorCodigo(String codigoOrden);

    Flux<OrdenMantenimientoResponse> listarTodas();

    Flux<OrdenMantenimientoResponse> listarPorRecurso(String recursoId);

    Flux<OrdenMantenimientoResponse> listarPorEntidad(String entidadId);

    Flux<OrdenMantenimientoResponse> listarPorEstado(EstadoOrden estado);
}
