package vg_ms.flota.orden.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelarOrdenRequest {

    @NotBlank(message = "El motivo de cancelación es obligatorio")
    private String motivo;
}
