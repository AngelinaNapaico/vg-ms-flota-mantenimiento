package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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
 * Inbound REST controller for managing the lifecycle of fleet maintenance
 * orders.
 * Exposes reactive endpoints for order registration, tracking, updates,
 * and technical discharge.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/maintenance-orders")
@RequiredArgsConstructor
@Tag(name = "Fleet Maintenance Orders", description = "Endpoints para la gestión reactiva del ciclo de vida de órdenes de mantenimiento de flota")
public class MaintenanceOrderRest {

        private final MaintenanceOrderUseCase useCase;

        // ═══════════════════════════════════════════════════
        // LECTURAS (READ_ROLES)
        // ═══════════════════════════════════════════════════

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
                        @Parameter(description = "Unique ID of the maintenance order") @PathVariable @NotBlank @Size(min = 1, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Invalid ID format") String id) {
                return useCase.findById(id)
                                .map(MaintenanceOrderRestMapper::toDetailResponse);
        }

        @GetMapping("/code/{code}")
        @PreAuthorize(RoleConstants.READ_ROLES)
        @Operation(summary = "Get maintenance order by tracking code")
        public Mono<MaintenanceOrderDetailResponse> getByCode(
                        @Parameter(description = "Order tracking code (e.g., ORD-2026-XXXX)") @PathVariable @NotBlank @Size(min = 1, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Invalid code format") String code) {
                return useCase.findByCode(code)
                                .map(MaintenanceOrderRestMapper::toDetailResponse);
        }

        @GetMapping("/resource/{resourceId}")
        @PreAuthorize(RoleConstants.READ_ROLES)
        @Operation(summary = "List maintenance history of an operational resource")
        public Flux<MaintenanceOrderResponse> getByResource(
                        @Parameter(description = "Operational resource ID") @PathVariable @NotBlank @Size(min = 1, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Invalid resource ID format") String resourceId) {
                return useCase.findByResource(resourceId)
                                .map(MaintenanceOrderRestMapper::toResponse);
        }

        @GetMapping("/entity/{entityId}")
        @PreAuthorize(RoleConstants.READ_ROLES)
        @Operation(summary = "List orders belonging to an entity (Police, Firefighters, Serenazgo)")
        public Flux<MaintenanceOrderResponse> getByEntity(
                        @Parameter(description = "Entity ID") @PathVariable @NotBlank @Size(min = 1, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Invalid entity ID format") String entityId) {
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

        // ═══════════════════════════════════════════════════
        // ESCRITURA (WRITE_ROLES)
        // ═══════════════════════════════════════════════════

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize(RoleConstants.WRITE_ROLES)
        @Operation(summary = "Create maintenance order and set vehicle to EN_MANTENIMIENTO")
        public Mono<MaintenanceOrderDetailResponse> create(
                        @Valid @RequestBody CreateMaintenanceOrderRequest request) {
                return SecurityUtils.getCurrentUsername()
                                .defaultIfEmpty("SYSTEM")
                                .flatMap(dni -> useCase.create(MaintenanceOrderRestMapper.toDomain(request, dni)))
                                .map(MaintenanceOrderRestMapper::toDetailResponse);
        }

        @PutMapping("/{id}")
        @PreAuthorize(RoleConstants.WRITE_ROLES)
        @Operation(summary = "Update failure reason, mileage, priority or workshop info of an open order")
        public Mono<MaintenanceOrderDetailResponse> update(
                        @PathVariable @NotBlank @Size(min = 1, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Invalid ID format") String id,
                        @Valid @RequestBody UpdateMaintenanceOrderRequest request) {
                return useCase.update(id, MaintenanceOrderRestMapper.toDomain(request))
                                .map(MaintenanceOrderRestMapper::toDetailResponse);
        }

        @PatchMapping("/{id}/assign-workshop")
        @PreAuthorize(RoleConstants.WRITE_ROLES)
        @Operation(summary = "Assign external workshop, technician and delivery date (IN_DIAGNOSIS)")
        public Mono<MaintenanceOrderDetailResponse> assignWorkshop(
                        @PathVariable @NotBlank @Size(min = 1, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Invalid ID format") String id,
                        @Valid @RequestBody AssignWorkshopRequest request) {
                return useCase.assignWorkshop(id, MaintenanceOrderRestMapper.toDomain(request))
                                .map(MaintenanceOrderRestMapper::toDetailResponse);
        }

        @PatchMapping("/{id}/start-repair")
        @PreAuthorize(RoleConstants.WRITE_ROLES)
        @Operation(summary = "Mark beginning of repair works in workshop (IN_WORKSHOP)")
        public Mono<MaintenanceOrderDetailResponse> startRepair(
                        @PathVariable @NotBlank @Size(min = 1, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Invalid ID format") String id) {
                return useCase.startRepair(id)
                                .map(MaintenanceOrderRestMapper::toDetailResponse);
        }

        @PatchMapping("/{id}/quality-control")
        @PreAuthorize(RoleConstants.WRITE_ROLES)
        @Operation(summary = "Pass vehicle to quality control and technical test stage (QUALITY_CONTROL)")
        public Mono<MaintenanceOrderDetailResponse> passToQualityControl(
                        @PathVariable @NotBlank @Size(min = 1, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Invalid ID format") String id) {
                return useCase.passToQualityControl(id)
                                .map(MaintenanceOrderRestMapper::toDetailResponse);
        }

        @PostMapping("/{id}/technical-discharge")
        @PreAuthorize(RoleConstants.WRITE_ROLES)
        @Operation(summary = "Issue technical discharge, complete order and restore resource to DISPONIBLE")
        public Mono<MaintenanceOrderDetailResponse> issueTechnicalDischarge(
                        @PathVariable @NotBlank @Size(min = 1, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Invalid ID format") String id,
                        @Valid @RequestBody TechnicalDischargeRequest request) {
                return SecurityUtils.getCurrentUsername()
                                .defaultIfEmpty("SYSTEM")
                                .flatMap(dni -> useCase.issueTechnicalDischarge(id,
                                                MaintenanceOrderRestMapper.toDomain(request, dni)))
                                .map(MaintenanceOrderRestMapper::toDetailResponse);
        }

        @PatchMapping("/{id}/cancel")
        @PreAuthorize(RoleConstants.WRITE_ROLES)
        @Operation(summary = "Cancel maintenance order and restore resource to DISPONIBLE")
        public Mono<MaintenanceOrderDetailResponse> cancel(
                        @PathVariable @NotBlank @Size(min = 1, max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Invalid ID format") String id,
                        @Valid @RequestBody CancelOrderRequest request) {
                return useCase.cancel(id, request.getReason(), "")
                                .map(MaintenanceOrderRestMapper::toDetailResponse);
        }
}
