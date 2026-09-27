package vg_ms.flota.orden.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value Object inmutable que representa los datos del taller mecánico y costos.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleTaller {

    private String nombreTaller;
    private String rucTaller;
    private String mecanicoResponsable;
    private String telefonoContacto;
    private BigDecimal costoEstimado;
    private BigDecimal costoFinal;
    private LocalDate fechaEstimadaEntrega;
    private String diagnosticoInicial;
}
