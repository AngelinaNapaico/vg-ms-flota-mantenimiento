package vg_ms.flota.orden.interfaces.rest;

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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vg_ms.flota.orden.application.OrdenMantenimientoService;
import vg_ms.flota.orden.domain.vo.EstadoOrden;
import vg_ms.flota.orden.interfaces.dto.*;
import vg_ms.flota.shared.security.RoleConstants;
import vg_ms.flota.shared.security.SecurityUtils;

@Slf4j
@RestController
@RequestMapping("/api/flota-mantenimiento/ordenes")
@RequiredArgsConstructor
@Tag(name = "Órdenes de Mantenimiento de Flota", description = "Gestión transaccional del ciclo de vida y siniestros de vehículos de emergencia")
public class OrdenMantenimientoController {

    private final OrdenMantenimientoService appService;

    // ─── Consultas ──────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Listar todas las órdenes de mantenimiento")
    public Flux<OrdenMantenimientoResponse> listarTodas() {
        log.info("GET /api/flota-mantenimiento/ordenes");
        return appService.listarTodas();
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Obtener orden de mantenimiento por su ID")
    public Mono<OrdenMantenimientoResponse> obtenerPorId(
            @Parameter(description = "ID único de la orden")
            @PathVariable String id) {
        log.info("GET /api/flota-mantenimiento/ordenes/{}", id);
        return appService.obtenerPorId(id);
    }

    @GetMapping("/codigo/{codigo}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Obtener orden por código de seguimiento (ej: ORD-2026-XXXX)")
    public Mono<OrdenMantenimientoResponse> obtenerPorCodigo(
            @Parameter(description = "Código de la orden")
            @PathVariable String codigo) {
        log.info("GET /api/flota-mantenimiento/ordenes/codigo/{}", codigo);
        return appService.obtenerPorCodigo(codigo);
    }

    @GetMapping("/recurso/{recursoId}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Listar historial de mantenimiento de un recurso operativo")
    public Flux<OrdenMantenimientoResponse> listarPorRecurso(
            @Parameter(description = "ID del recurso operativo")
            @PathVariable String recursoId) {
        log.info("GET /api/flota-mantenimiento/ordenes/recurso/{}", recursoId);
        return appService.listarPorRecurso(recursoId);
    }

    @GetMapping("/entidad/{entidadId}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Listar órdenes de mantenimiento por entidad")
    public Flux<OrdenMantenimientoResponse> listarPorEntidad(
            @Parameter(description = "ID de la entidad (comisaría, bomberos, hospital)")
            @PathVariable String entidadId) {
        log.info("GET /api/flota-mantenimiento/ordenes/entidad/{}", entidadId);
        return appService.listarPorEntidad(entidadId);
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Listar órdenes filtradas por estado")
    public Flux<OrdenMantenimientoResponse> listarPorEstado(
            @Parameter(description = "Estado de la orden")
            @PathVariable EstadoOrden estado) {
        log.info("GET /api/flota-mantenimiento/ordenes/estado/{}", estado);
        return appService.listarPorEstado(estado);
    }

    // ─── Transacciones de Negocio ───────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Crear orden de mantenimiento / reporte de siniestro e inhabilitar recurso")
    public Mono<OrdenMantenimientoResponse> crearOrden(
            @Valid @RequestBody CrearOrdenRequest request,
            ServerWebExchange exchange) {
        log.info("POST /api/flota-mantenimiento/ordenes - Recurso: {}", request.getRecursoId());
        String token = extractToken(exchange);

        return SecurityUtils.getCurrentUsername()
                .flatMap(dni -> appService.crearOrden(request, token, dni));
    }

    @PatchMapping("/{id}/taller")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Asignar taller mecánico, diagnóstico inicial y presupuesto")
    public Mono<OrdenMantenimientoResponse> asignarTaller(
            @PathVariable String id,
            @Valid @RequestBody AsignarTallerRequest request) {
        log.info("PATCH /api/flota-mantenimiento/ordenes/{}/taller", id);
        return appService.asignarTaller(id, request);
    }

    @PatchMapping("/{id}/iniciar-reparacion")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Marcar inicio de los trabajos mecánicos en taller (EN_TALLER)")
    public Mono<OrdenMantenimientoResponse> iniciarReparacion(@PathVariable String id) {
        log.info("PATCH /api/flota-mantenimiento/ordenes/{}/iniciar-reparacion", id);
        return appService.iniciarReparacion(id);
    }

    @PatchMapping("/{id}/control-calidad")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Pasar vehículo a pruebas técnicas y control de calidad post-reparación")
    public Mono<OrdenMantenimientoResponse> pasarAControlCalidad(@PathVariable String id) {
        log.info("PATCH /api/flota-mantenimiento/ordenes/{}/control-calidad", id);
        return appService.pasarAControlCalidad(id);
    }

    @PostMapping("/{id}/alta-operativa")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Emitir dictamen de alta técnica, finalizar orden y habilitar vehículo a DISPONIBLE")
    public Mono<OrdenMantenimientoResponse> emitirAltaOperativa(
            @PathVariable String id,
            @Valid @RequestBody AltaOperativaRequest request,
            ServerWebExchange exchange) {
        log.info("POST /api/flota-mantenimiento/ordenes/{}/alta-operativa", id);
        String token = extractToken(exchange);

        return SecurityUtils.getCurrentUsername()
                .flatMap(dni -> appService.emitirAltaOperativa(id, request, token, dni));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Cancelar orden de mantenimiento y rehabilitar recurso")
    public Mono<OrdenMantenimientoResponse> cancelarOrden(
            @PathVariable String id,
            @Valid @RequestBody CancelarOrdenRequest request,
            ServerWebExchange exchange) {
        log.info("PATCH /api/flota-mantenimiento/ordenes/{}/cancelar", id);
        String token = extractToken(exchange);
        return appService.cancelarOrden(id, request, token);
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return "";
    }
}
