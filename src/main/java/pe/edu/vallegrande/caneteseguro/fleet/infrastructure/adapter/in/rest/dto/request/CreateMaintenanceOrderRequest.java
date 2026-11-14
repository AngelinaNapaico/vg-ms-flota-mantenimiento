package pe.edu.vallegrande.caneteseguro.fleet.infrastructure.adapter.in.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.caneteseguro.fleet.domain.vo.MaintenanceType;
import pe.edu.vallegrande.caneteseguro.fleet.domain.vo.OrderPriority;

/**
 * DTO para la creación de una nueva orden de mantenimiento.
 * Soporta nombres en inglés y español vía alias JSON para compatibilidad total (STD-075).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para registrar una orden de mantenimiento de flota")
public class CreateMaintenanceOrderRequest {

    @NotBlank(message = "El ID del recurso operativo es obligatorio")
    @Size(max = 100, message = "El ID del recurso no puede exceder 100 caracteres")
    @JsonProperty("resourceId")
    @JsonAlias({"recursoId", "resourceId"})
    @Schema(description = "Identificador del recurso en vg-ms-recursos", example = "68ef45d12a8934bc78901234")
    private String resourceId;

    @Size(max = 20, message = "La placa no puede exceder 20 caracteres")
    @JsonProperty("resourcePlate")
    @JsonAlias({"recursoPlaca", "resourcePlate"})
    @Schema(description = "Placa o identificador visible del vehículo", example = "POL-102")
    private String resourcePlate;

    @Size(max = 100, message = "El ID de entidad no puede exceder 100 caracteres")
    @JsonProperty("entityId")
    @JsonAlias({"entidadId", "entityId"})
    @Schema(description = "ID de la entidad propietaria", example = "65ab12c98d7123ef45678901")
    private String entityId;

    @Size(max = 100, message = "El ID del incidente no puede exceder 100 caracteres")
    @JsonProperty("incidentId")
    @JsonAlias({"incidenteId", "incidentId"})
    @Schema(description = "ID del incidente si proviene de un siniestro", example = "7b5f792c7c84938ece21b113")
    private String incidentId;

    @NotNull(message = "El tipo de mantenimiento es obligatorio")
    @JsonProperty("maintenanceType")
    @JsonAlias({"tipoMantenimiento", "maintenanceType"})
    @Schema(description = "Naturaleza de la intervención mecánica", example = "CORRECTIVO_ACCIDENTE")
    private MaintenanceType maintenanceType;

    @NotNull(message = "La prioridad de atención es obligatoria")
    @JsonProperty("priority")
    @JsonAlias({"prioridad", "priority"})
    @Schema(description = "Nivel de urgencia operativa", example = "ALTA")
    private OrderPriority priority;

    @NotBlank(message = "El motivo de la falla o solicitud es obligatorio")
    @Size(max = 1000, message = "El motivo de la falla no puede exceder 1000 caracteres")
    @JsonProperty("failureReason")
    @JsonAlias({"motivoFalla", "failureReason"})
    @Schema(description = "Descripción de la avería, síntoma o requerimiento técnico", example = "Falla de encendido y pérdida de presión en frenos tras persecución")
    private String failureReason;

    @Min(value = 0, message = "El kilometraje reportado debe ser mayor o igual a cero")
    @JsonProperty("reportedMileage")
    @JsonAlias({"kilometrajeReportado", "reportedMileage"})
    @Schema(description = "Odómetro actual del móvil", example = "84500")
    private Integer reportedMileage;

    @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres")
    @JsonProperty("remarks")
    @JsonAlias({"observaciones", "remarks"})
    @Schema(description = "Notas adicionales de la guardia o del chofer", example = "Vehículo inmovilizado en maestranza de Cañete")
    private String remarks;
}
