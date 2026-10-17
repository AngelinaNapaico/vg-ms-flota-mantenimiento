package pe.edu.vallegrande.flota.infrastructure.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para registrar el informe de alta operativa técnica y liquidación final de la reparación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de aprobación técnica y entrega del vehículo")
public class AltaOperativaRequest {

    @NotBlank(message = "El dictamen técnico de conformidad es obligatorio")
    @Schema(description = "Dictamen del perito mecánico o jefe de maestranza", example = "Vehículo probado en ruta; sistema de frenos y motor al 100% operativo")
    private String dictamenTecnico;

    @Schema(description = "Pruebas mecánicas efectuadas antes de la entrega", example = "Prueba de frenado a 60km/h y chequeo de temperatura por 30 minutos")
    private String pruebasRealizadas;

    @NotNull(message = "El kilometraje de salida es obligatorio")
    @PositiveOrZero(message = "El kilometraje de salida debe ser mayor o igual a cero")
    @Schema(description = "Odómetro con el que la unidad abandona el taller", example = "85415")
    private Integer kilometrajeSalida;

    @Schema(description = "Costo final total facturado por la reparación", example = "920.00")
    private BigDecimal costoFinal;

    @Schema(description = "Observaciones finales de mantenimiento", example = "Próximo mantenimiento preventivo programado a los 90,000 km")
    private String observaciones;
}
