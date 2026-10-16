package pe.edu.vallegrande.flota.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value Object inmutable que representa los datos del taller mecánico, diagnóstico y presupuesto.
 * Estándar PR241 - Dominio puro sin anotaciones de persistencia.
 */
@Getter
@Setter
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
