package pe.edu.vallegrande.flota.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estados del ciclo de vida de una orden de mantenimiento vehicular.
 * Según estándar PR241 - Arquitectura Hexagonal.
 */
@Schema(description = "Estados de la orden de mantenimiento")
public enum EstadoOrden {
    @Schema(description = "Orden registrada por el chofer/operador de la entidad")
    REGISTRADA,

    @Schema(description = "Vehículo ingresado a taller para diagnóstico y presupuesto")
    EN_DIAGNOSTICO,

    @Schema(description = "En proceso activo de reparación y cambio de repuestos")
    EN_TALLER,

    @Schema(description = "Reparación completada, en pruebas técnicas y control de calidad")
    CONTROL_CALIDAD,

    @Schema(description = "Alta técnica emitida y vehículo habilitado nuevamente a DISPONIBLE")
    FINALIZADA,

    @Schema(description = "Orden cancelada por error de registro o resolución externa")
    CANCELADA
}
