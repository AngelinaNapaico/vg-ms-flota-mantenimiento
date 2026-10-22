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
import pe.edu.vallegrande.flota.domain.models.OrdenMantenimiento;
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
import pe.edu.vallegrande.flota.infrastructure.security.TenantScope;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller reactivo para órdenes de mantenimiento de flota según estándar PR241.
 * Nombre de clase estandarizado con sufijo Rest (*Rest).
 * Implementa seguridad multitenant restringida a la Entidad propietaria (o SUPER_ADMIN).
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
    @Operation(summary = "Listar todas las órdenes de mantenimiento según alcance de entidad")
    public Flux<OrdenMantenimientoResponse> getAll() {
        return porAlcance()
                .map(OrdenMantenimientoMapper::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Obtener orden de mantenimiento por su ID")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> getById(
            @Parameter(description = "ID único de la orden")
            @PathVariable String id) {
        return visible(service.findById(id))
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
        return visible(service.findByCodigo(codigo))
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
        return visibles(service.findByRecurso(recursoId))
                .map(OrdenMantenimientoMapper::toResponse);
    }

    @GetMapping("/entidad/{entidadId}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Listar órdenes de mantenimiento por entidad")
    public Flux<OrdenMantenimientoResponse> getByEntidad(
            @Parameter(description = "ID de la entidad (comisaría, bomberos, hospital)")
            @PathVariable String entidadId) {
        return TenantScope.current().flatMapMany(scope -> {
            if (scope.all() || (scope.entidadId() != null && scope.entidadId().equals(entidadId))) {
                return service.findByEntidad(entidadId);
            }
            return Flux.empty();
        }).map(OrdenMantenimientoMapper::toResponse);
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Listar órdenes filtradas por estado")
    public Flux<OrdenMantenimientoResponse> getByEstado(
            @Parameter(description = "Estado de la orden")
            @PathVariable EstadoOrden estado) {
        return visibles(service.findByEstado(estado))
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

        return TenantScope.current().flatMap(scope -> {
            scope.requireWriter();
            if (!scope.all() && scope.entidadId() != null) {
                request.setEntidadId(scope.entidadId());
            }
            return SecurityUtils.getCurrentUsername()
                    .flatMap(dni -> service.create(request, token, dni))
                    .map(OrdenMantenimientoMapper::toDetailResponse)
                    .map(data -> ResponseDto.success(data, "Orden de mantenimiento registrada exitosamente"));
        });
    }

    @PatchMapping("/{id}/taller")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Asignar taller mecánico, diagnóstico inicial y presupuesto")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> asignarTaller(
            @PathVariable String id,
            @Valid @RequestBody AsignarTallerRequest request) {
        return propio(id)
                .flatMap(scope -> service.asignarTaller(id, request))
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(data -> ResponseDto.success(data, "Taller y presupuesto asignados exitosamente"));
    }

    @PatchMapping("/{id}/iniciar-reparacion")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Marcar inicio de los trabajos mecánicos en taller (EN_TALLER)")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> iniciarReparacion(@PathVariable String id) {
        return propio(id)
                .flatMap(scope -> service.iniciarReparacion(id))
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(data -> ResponseDto.success(data, "Reparación iniciada en taller"));
    }

    @PatchMapping("/{id}/control-calidad")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Pasar vehículo a pruebas técnicas y control de calidad")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> pasarAControlCalidad(@PathVariable String id) {
        return propio(id)
                .flatMap(scope -> service.pasarAControlCalidad(id))
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

        return propio(id).flatMap(scope -> SecurityUtils.getCurrentUsername()
                .flatMap(dni -> service.emitirAltaOperativa(id, request, token, dni))
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(data -> ResponseDto.success(data, "Alta técnica emitida exitosamente; recurso habilitado a DISPONIBLE")));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Cancelar orden de mantenimiento y rehabilitar recurso")
    public Mono<ResponseDto<OrdenMantenimientoDetailResponse>> cancelar(
            @PathVariable String id,
            @Valid @RequestBody CancelarOrdenRequest request,
            ServerWebExchange exchange) {
        String token = extractToken(exchange);

        return propio(id).flatMap(scope -> service.cancelar(id, request, token)
                .map(OrdenMantenimientoMapper::toDetailResponse)
                .map(data -> ResponseDto.success(data, "Orden de mantenimiento cancelada")));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ALCANCE MULTITENANT (ENTIDAD / SUPER_ADMIN)
    // ═══════════════════════════════════════════════════════════════════════════

    private Flux<OrdenMantenimiento> porAlcance() {
        return TenantScope.current().flatMapMany(scope -> {
            if (scope.all()) {
                return service.findAll();
            }
            if (scope.entidadId() != null) {
                return service.findByEntidad(scope.entidadId());
            }
            return Flux.empty();
        });
    }

    private static Mono<OrdenMantenimiento> visible(Mono<OrdenMantenimiento> ordenMono) {
        return TenantScope.current().flatMap(scope ->
                ordenMono.filter(orden -> scope.canRead(orden.getEntidadId())));
    }

    private static Flux<OrdenMantenimiento> visibles(Flux<OrdenMantenimiento> ordenesFlux) {
        return TenantScope.current().flatMapMany(scope ->
                ordenesFlux.filter(orden -> scope.canRead(orden.getEntidadId())));
    }

    private Mono<TenantScope> propio(String id) {
        return TenantScope.current().flatMap(scope -> {
            scope.requireWriter();
            if (scope.all()) {
                return Mono.just(scope);
            }
            return service.findById(id)
                    .filter(orden -> scope.canRead(orden.getEntidadId()))
                    .switchIfEmpty(Mono.error(TenantScope.forbidden()))
                    .thenReturn(scope);
        });
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return "";
    }
}
