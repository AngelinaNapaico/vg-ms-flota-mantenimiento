package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for recording technical discharge report and vehicle delivery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de aprobación técnica y entrega del vehículo")
public class TechnicalDischargeRequest {

    @NotBlank(message = "El dictamen técnico de conformidad es obligatorio")
    @Size(max = 1000, message = "El dictamen técnico no puede exceder 1000 caracteres")
    @JsonProperty("technicalVerdict")
    @JsonAlias({"dictamenTecnico", "technicalVerdict"})
    @Schema(description = "Dictamen del perito mecánico o jefe de maestranza", example = "Vehículo probado en ruta; sistema de frenos y motor al 100% operativo")
    private String technicalVerdict;

    @Size(max = 1000, message = "Las pruebas realizadas no pueden exceder 1000 caracteres")
    @JsonProperty("testsPerformed")
    @JsonAlias({"pruebasRealizadas", "testsPerformed"})
    @Schema(description = "Pruebas mecánicas efectuadas antes de la entrega", example = "Prueba de frenado a 60km/h y chequeo de temperatura por 30 minutos")
    private String testsPerformed;

    @NotNull(message = "El kilometraje de salida es obligatorio")
    @PositiveOrZero(message = "El kilometraje de salida debe ser mayor o igual a cero")
    @JsonProperty("exitMileage")
    @JsonAlias({"kilometrajeSalida", "exitMileage"})
    @Schema(description = "Odómetro con el que la unidad abandona el taller", example = "85415")
    private Integer exitMileage;

    @PositiveOrZero(message = "El costo final no puede ser negativo")
    @JsonProperty("finalCost")
    @JsonAlias({"costoFinal", "finalCost"})
    @Schema(description = "Costo final total facturado por la reparación", example = "920.00")
    private BigDecimal finalCost;

    @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres")
    @JsonProperty("remarks")
    @JsonAlias({"observaciones", "remarks"})
    @Schema(description = "Observaciones de entrega y garantía", example = "Garantía de 6 meses en repuestos y mano de obra")
    private String remarks;
}
