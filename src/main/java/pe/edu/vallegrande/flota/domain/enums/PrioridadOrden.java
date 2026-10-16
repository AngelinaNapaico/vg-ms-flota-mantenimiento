package pe.edu.vallegrande.flota.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Nivel de prioridad operativa de atención de la orden de reparación.
 */
@Schema(description = "Nivel de urgencia o prioridad de la orden")
public enum PrioridadOrden {
    @Schema(description = "Prioridad baja - no compromete la movilidad inmediata")
    BAJA,

    @Schema(description = "Prioridad media - requiere atención dentro del turno")
    MEDIA,

    @Schema(description = "Prioridad alta - unidad crítica fuera de servicio que atiende emergencias de alto impacto")
    ALTA,

    @Schema(description = "Prioridad urgente - vehículo de primera respuesta indispensable")
    URGENTE
}
