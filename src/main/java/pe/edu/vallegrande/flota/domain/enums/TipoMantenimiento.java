package pe.edu.vallegrande.flota.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Clasificación del tipo de intervención técnica vehicular.
 */
@Schema(description = "Tipo de intervención de mantenimiento")
public enum TipoMantenimiento {
    @Schema(description = "Mantenimiento preventivo programado (aceite, filtros, neumáticos)")
    PREVENTIVO,

    @Schema(description = "Mantenimiento correctivo por avería mecánica imprevista")
    CORRECTIVO,

    @Schema(description = "Mantenimiento derivado de accidente o siniestro durante atención de emergencia")
    CORRECTIVO_SINIESTRO
}
