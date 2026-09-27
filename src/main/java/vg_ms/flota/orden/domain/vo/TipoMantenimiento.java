package vg_ms.flota.orden.domain.vo;

/**
 * Naturaleza del mantenimiento del recurso operativo.
 */
public enum TipoMantenimiento {
    PREVENTIVO,            // Cambio de aceite, neumáticos, frenos rutinario
    CORRECTIVO_SINIESTRO,   // Choque o siniestro ocurrido durante la atención de un incidente
    URGENCIA_MECANICA      // Desperfecto repentino en motor, transmisión o sistema eléctrico
}
