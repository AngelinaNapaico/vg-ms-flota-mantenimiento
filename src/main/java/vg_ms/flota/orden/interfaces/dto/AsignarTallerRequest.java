package vg_ms.flota.orden.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignarTallerRequest {

    @NotBlank(message = "El nombre o razón social del taller es obligatorio")
    private String nombreTaller;

    private String rucTaller;
    private String mecanicoResponsable;
    private String telefonoContacto;
    private BigDecimal costoEstimado;
    private LocalDate fechaEstimadaEntrega;
    private String diagnosticoInicial;
}
