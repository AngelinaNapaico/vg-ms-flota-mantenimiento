package pe.edu.vallegrande.flota.infrastructure.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.flota.domain.enums.PrioridadOrden;
import pe.edu.vallegrande.flota.domain.enums.TipoMantenimiento;

/**
 * DTO para la creación de una orden de mantenimiento vehicular.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para registrar una orden de mantenimiento")
public class CreateOrdenMantenimientoRequest {

    @NotBlank(message = "El ID del recurso operativo es obligatorio")
    @Schema(description = "ID del recurso operativo a ingresar a taller", example = "64a4eb5a0bc35225f9459ce2")
    private String recursoId;

    @Schema(description = "Placa visible del vehículo (ej: patrulla o ambulancia)", example = "EUA-911")
    private String recursoPlaca;

    @NotBlank(message = "El ID de la entidad responsable es obligatorio")
    @Schema(description = "ID de la comisaría, estación o centro de salud", example = "ENT-001")
    private String entidadId;

    @Schema(description = "ID del incidente durante el cual ocurrió la avería o colisión (opcional)", example = "INC-2026-003")
    private String incidenteId;

    @NotNull(message = "El tipo de mantenimiento es obligatorio")
    @Schema(description = "Clasificación de la intervención", example = "CORRECTIVO")
    private TipoMantenimiento tipoMantenimiento;

    @Schema(description = "Nivel de prioridad operativa", example = "ALTA")
    private PrioridadOrden prioridad;

    @NotBlank(message = "El motivo o síntoma de la avería es obligatorio")
    @Schema(description = "Descripción detallada del desperfecto mecánico", example = "Falla en sistema de frenos y recalentamiento de motor")
    private String motivoAveria;

    @PositiveOrZero(message = "El kilometraje debe ser mayor o igual a cero")
    @Schema(description = "Kilometraje registrado al momento del ingreso", example = "85400")
    private Integer kilometrajeReportado;

    @Schema(description = "Observaciones adicionales del chofer o despachador", example = "Remolcado con grúa municipal")
    private String observaciones;
}
