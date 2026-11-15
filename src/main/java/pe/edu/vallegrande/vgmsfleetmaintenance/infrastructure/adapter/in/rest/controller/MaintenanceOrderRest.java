package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.controller;

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
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.in.MaintenanceOrderUseCase;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderStatus;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.request.AssignWorkshopRequest;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.request.CancelOrderRequest;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.request.CreateMaintenanceOrderRequest;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.request.TechnicalDischargeRequest;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.request.UpdateMaintenanceOrderRequest;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.response.MaintenanceOrderDetailResponse;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.response.MaintenanceOrderResponse;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.mapper.MaintenanceOrderRestMapper;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security.RoleConstants;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security.SecurityUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Primary Inbound REST Controller for Fleet Maintenance Orders.
 * 100% compliant with Hexagonal, Reactive WebFlux, STD-028 and AGENTS.md standards:
 * - Direct Mono/Flux publisher return types without domain model exposure.
 * - Centralized exception handling via GlobalExceptionHandler.
 * - Pure English REST API standard.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/maintenance-orders")
@RequiredArgsConstructor
@Tag(name = "Maintenance Orders", description = "Lifecycle and repair management for emergency fleet vehicles")
public class MaintenanceOrderRest {

    private final MaintenanceOrderUseCase useCase;

    @GetMapping
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "List all maintenance orders scoped to entity tenant")
    public Flux<MaintenanceOrderResponse> getAll() {
        return useCase.findAll()
                .map(MaintenanceOrderRestMapper::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Get maintenance order by unique ID")
    public Mono<MaintenanceOrderDetailResponse> getById(
            @Parameter(description = "Unique ID of the maintenance order") @PathVariable String id) {
        return useCase.findById(id)
                .map(MaintenanceOrderRestMapper::toDetailResponse);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "Get maintenance order by tracking code")
    public Mono<MaintenanceOrderDetailResponse> getByCode(
            @Parameter(description = "Order tracking code (e.g., ORD-2026-XXXX)") @PathVariable String code) {
        return useCase.findByCode(code)
                .map(MaintenanceOrderRestMapper::toDetailResponse);
    }

    @GetMapping("/resource/{resourceId}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "List maintenance history of an operational resource")
    public Flux<MaintenanceOrderResponse> getByResource(
            @Parameter(description = "Operational resource ID") @PathVariable String resourceId) {
        return useCase.findByResource(resourceId)
                .map(MaintenanceOrderRestMapper::toResponse);
    }

    @GetMapping("/entity/{entityId}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "List orders belonging to an entity (Police, Firefighters, Serenazgo)")
    public Flux<MaintenanceOrderResponse> getByEntity(
            @Parameter(description = "Entity ID") @PathVariable String entityId) {
        return useCase.findByEntity(entityId)
                .map(MaintenanceOrderRestMapper::toResponse);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize(RoleConstants.READ_ROLES)
    @Operation(summary = "List orders filtered by lifecycle status")
    public Flux<MaintenanceOrderResponse> getByStatus(
            @Parameter(description = "Order lifecycle status") @PathVariable OrderStatus status) {
        return useCase.findByStatus(status)
                .map(MaintenanceOrderRestMapper::toResponse);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Create maintenance order and set vehicle to EN_MANTENIMIENTO")
    public Mono<MaintenanceOrderDetailResponse> create(
            @Valid @RequestBody CreateMaintenanceOrderRequest request,
            ServerWebExchange exchange) {
        String token = extractToken(exchange);

        return SecurityUtils.getCurrentUsername()
                .flatMap(dni -> useCase.create(MaintenanceOrderRestMapper.toDomain(request, dni), token))
                .map(MaintenanceOrderRestMapper::toDetailResponse);
    }

    @PutMapping("/{id}")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Update failure reason, mileage, priority or workshop info of an open order")
    public Mono<MaintenanceOrderDetailResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateMaintenanceOrderRequest request) {
        return useCase.update(id, MaintenanceOrderRestMapper.toDomain(request))
                .map(MaintenanceOrderRestMapper::toDetailResponse);
    }

    @PatchMapping("/{id}/assign-workshop")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Assign external workshop, technician and delivery date (IN_DIAGNOSIS)")
    public Mono<MaintenanceOrderDetailResponse> assignWorkshop(
            @PathVariable String id,
            @Valid @RequestBody AssignWorkshopRequest request) {
        return useCase.assignWorkshop(id, MaintenanceOrderRestMapper.toDomain(request))
                .map(MaintenanceOrderRestMapper::toDetailResponse);
    }

    @PatchMapping("/{id}/start-repair")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Mark beginning of repair works in workshop (IN_WORKSHOP)")
    public Mono<MaintenanceOrderDetailResponse> startRepair(@PathVariable String id) {
        return useCase.startRepair(id)
                .map(MaintenanceOrderRestMapper::toDetailResponse);
    }

    @PatchMapping("/{id}/quality-control")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Pass vehicle to quality control and technical test stage (QUALITY_CONTROL)")
    public Mono<MaintenanceOrderDetailResponse> passToQualityControl(@PathVariable String id) {
        return useCase.passToQualityControl(id)
                .map(MaintenanceOrderRestMapper::toDetailResponse);
    }

    @PostMapping("/{id}/technical-discharge")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Issue technical discharge, complete order and restore resource to DISPONIBLE")
    public Mono<MaintenanceOrderDetailResponse> issueTechnicalDischarge(
            @PathVariable String id,
            @Valid @RequestBody TechnicalDischargeRequest request,
            ServerWebExchange exchange) {
        String token = extractToken(exchange);

        return SecurityUtils.getCurrentUsername()
                .flatMap(dni -> useCase.issueTechnicalDischarge(id, MaintenanceOrderRestMapper.toDomain(request, dni), token))
                .map(MaintenanceOrderRestMapper::toDetailResponse);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize(RoleConstants.WRITE_ROLES)
    @Operation(summary = "Cancel maintenance order and restore resource to DISPONIBLE")
    public Mono<MaintenanceOrderDetailResponse> cancel(
            @PathVariable String id,
            @Valid @RequestBody CancelOrderRequest request,
            ServerWebExchange exchange) {
        String token = extractToken(exchange);

        return useCase.cancel(id, request.getReason(), "", token)
                .map(MaintenanceOrderRestMapper::toDetailResponse);
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return "";
    }
}
