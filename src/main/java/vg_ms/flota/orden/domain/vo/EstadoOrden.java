package vg_ms.flota.orden.domain.vo;

/**
 * Estados del ciclo de vida de una orden de mantenimiento de flota.
 */
public enum EstadoOrden {
    REGISTRADA,          // Orden abierta, vehículo reportado con avería o siniestro
    EN_DIAGNOSTICO,      // En revisión inicial para evaluación de daños o presupuesto
    EN_TALLER,           // Reparación o mantenimiento en ejecución activa
    CONTROL_CALIDAD,     // Pruebas de funcionamiento post-reparación
    FINALIZADA,          // Alta técnica emitida y vehículo devuelto a DISPONIBLE
    CANCELADA            // Desestimada o cancelada administrativamente
}
