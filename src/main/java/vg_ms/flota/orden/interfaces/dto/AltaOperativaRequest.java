package vg_ms.flota.orden.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AltaOperativaRequest {

    @NotBlank(message = "El dictamen técnico de conformidad es obligatorio")
    private String dictamenTecnico;

    private String pruebasRealizadas;

    private Integer kilometrajeSalida;

    private BigDecimal costoFinal;

    private String observaciones;
}
