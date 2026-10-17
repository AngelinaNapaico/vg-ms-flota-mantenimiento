package pe.edu.vallegrande.flota.infrastructure.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para asignar el taller mecánico y presupuesto inicial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos del taller mecánico y presupuesto acordado")
public class AsignarTallerRequest {

    @NotBlank(message = "El nombre del taller mecánico es obligatorio")
    @Schema(description = "Razón social del taller contratado", example = "Mecánica Automotriz San Vicente S.A.C.")
    private String nombreTaller;

    @Schema(description = "RUC del taller mecánico", example = "20601234567")
    private String rucTaller;

    @Schema(description = "Nombre del mecánico o jefe de taller a cargo", example = "Carlos Mendoza")
    private String mecanicoResponsable;

    @Schema(description = "Teléfono de contacto del taller", example = "987654321")
    private String telefonoContacto;

    @NotNull(message = "El costo estimado de la reparación es obligatorio")
    @Positive(message = "El costo estimado debe ser mayor a cero")
    @Schema(description = "Presupuesto inicial cotizado por el taller", example = "850.00")
    private BigDecimal costoEstimado;

    @Schema(description = "Fecha prometida de entrega del vehículo reparado", example = "2026-10-25")
    private LocalDate fechaEstimadaEntrega;

    @Schema(description = "Diagnóstico técnico inicial del taller", example = "Desgaste severo en pastillas de freno y cambio de manguera de radiador")
    private String diagnosticoInicial;
}
