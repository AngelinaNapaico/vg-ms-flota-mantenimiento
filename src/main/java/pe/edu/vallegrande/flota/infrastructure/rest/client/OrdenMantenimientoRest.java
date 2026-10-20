package pe.edu.vallegrande.flota.infrastructure.rest.client;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pe.edu.vallegrande.flota.application.services.OrdenMantenimientoService;
import pe.edu.vallegrande.flota.domain.enums.EstadoOrden;
import pe.edu.vallegrande.flota.infrastructure.dto.common.ResponseDto;
import pe.edu.vallegrande.flota.infrastructure.dto.request.AltaOperativaRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.request.AsignarTallerRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.request.CancelarOrdenRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.request.CreateOrdenMantenimientoRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.response.OrdenMantenimientoDetailResponse;
import pe.edu.vallegrande.flota.infrastructure.dto.response.OrdenMantenimientoResponse;
import pe.edu.vallegrande.flota.infrastructure.mapper.OrdenMantenimientoMapper;
import pe.edu.vallegrande.flota.infrastructure.security.RoleConstants;
import pe.edu.vallegrande.flota.infrastructure.security.SecurityUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller reactivo para órdenes de mantenimiento de flota según estándar PR241.
 * Nombre de clase estandarizado con sufijo Rest (*Rest).
 */
@Slf4j
@RestController
@RequestMapping("/api/flota-mantenimiento/ordenes")
@RequiredArgsConstructor
@Tag(name = "Órdenes de Mantenimiento de Flota", description = "Gestión transaccional del ciclo de vida y siniestros de vehículos de emergencia")
public class OrdenMantenimientoRest {

    private final OrdenMantenimientoService service;

    @GetMapping
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Listar todas las órdenes de mantenimiento")
    public Flux<OrdenMantenimientoResponse> getAll() {
        return service.findAll()
                .map(OrdenMantenimientoMapper::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Obtener orden de mantenimiento por su ID")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> getById(
            @Parameter(description = "ID único de la orden")
            @PathVariable String id) {
        return service.findById(id)
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(ResponseDto::success)
                .defaultIfEmpty(ResponseDto.error("Orden de mantenimiento no encontrada"));
    }

    @GetMapping("/codigo/{codigo}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Obtener orden por código de seguimiento")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> getByCodigo(
            @Parameter(description = "Código de la orden (ej: ORD-2026-XXXX)")
            @PathVariable String codigo) {
        return service.findByCodigo(codigo)
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(ResponseDto::success)
                .defaultIfEmpty(ResponseDto.error("Orden no encontrada"));
    }

    @GetMapping("/recurso/{recursoId}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Listar historial de mantenimiento de un recurso operativo")
    public Flux<OrdenMantenimientoResponse> getByRecurso(
            @Parameter(description = "ID del recurso operativo")
            @PathVariable String recursoId) {
        return service.findByRecurso(recursoId)
                .map(OrdenMantenimientoMapper::toResponse);
    }

    @GetMapping("/entidad/{entidadId}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Listar órdenes de mantenimiento por entidad")
    public Flux<OrdenMantenimientoResponse> getByEntidad(
            @Parameter(description = "ID de la entidad (comisaría, bomberos, hospital)")
            @PathVariable String entidadId) {
        return service.findByEntidad(entidadId)
                .map(OrdenMantenimientoMapper::toResponse);
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Listar órdenes filtradas por estado")
    public Flux<OrdenMantenimientoResponse> getByEstado(
            @Parameter(description = "Estado de la orden")
            @PathVariable EstadoOrden estado) {
        return service.findByEstado(estado)
                .map(OrdenMantenimientoMapper::toResponse);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Crear orden de mantenimiento e inhabilitar recurso en vg-ms-recursos")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> create(
            @Valid @RequestBody CreateOrdenMantenimientoRequest request,
            ServerWebExchange exchange) {
        String token = extractToken(exchange);

        return SecurityUtils.getCurrentUsername()
                .flatMap(dni -> service.create(request, token, dni))
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(data -> ResponseDto.success(data, "Orden de mantenimiento registrada exitosamente"));
    }

    @PatchMapping("/{id}/taller")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Asignar taller mecánico, diagnóstico inicial y presupuesto")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> asignarTaller(
            @PathVariable String id,
            @Valid @RequestBody AsignarTallerRequest request) {
        return service.asignarTaller(id, request)
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(data -> ResponseDto.success(data, "Taller y presupuesto asignados exitosamente"));
    }

    @PatchMapping("/{id}/iniciar-reparacion")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Marcar inicio de los trabajos mecánicos en taller (EN_TALLER)")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> iniciarReparacion(@PathVariable String id) {
        return service.iniciarReparacion(id)
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(data -> ResponseDto.success(data, "Reparación iniciada en taller"));
    }

    @PatchMapping("/{id}/control-calidad")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Pasar vehículo a pruebas técnicas y control de calidad")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> pasarAControlCalidad(@PathVariable String id) {
        return service.pasarAControlCalidad(id)
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(data -> ResponseDto.success(data, "Vehículo en control de calidad y pruebas técnicas"));
    }

    @PostMapping("/{id}/alta-operativa")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Emitir alta técnica, finalizar orden y rehabilitar vehículo a DISPONIBLE")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> emitirAltaOperativa(
            @PathVariable String id,
            @Valid @RequestBody AltaOperativaRequest request,
            ServerWebExchange exchange) {
        String token = extractToken(exchange);

        return SecurityUtils.getCurrentUsername()
                .flatMap(dni -> service.emitirAltaOperativa(id, request, token, dni))
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(data -> ResponseDto.success(data, "Alta técnica emitida exitosamente; recurso habilitado a DISPONIBLE"));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Cancelar orden de mantenimiento y rehabilitar recurso")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> cancelar(
            @PathVariable String id,
            @Valid @RequestBody CancelarOrdenRequest request,
            ServerWebExchange exchange) {
        String token = extractToken(exchange);

        return service.cancelar(id, request, token)
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(data -> ResponseDto.success(data, "Orden de mantenimiento cancelada"));
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return "";
    }
}
