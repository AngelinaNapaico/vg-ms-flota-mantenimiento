package pe.edu.vallegrande.flota.infrastructure.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la cancelación justificada de una orden de mantenimiento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Motivo de cancelación de la orden")
public class CancelarOrdenRequest {

    @NotBlank(message = "El motivo de la cancelación es obligatorio")
    @Schema(description = "Justificación administrativa o técnica", example = "Registro duplicado; unidad fue reparada in situ")
    private String motivo;
}
