package pe.edu.vallegrande.caneteseguro.flota.domain.vo;

/**
 * Estados del ciclo de vida operativo de una orden de mantenimiento.
 * Convención en inglés según estándar DatabaseStorage (NOM-01, NOM-03).
 */
public enum OrderStatus {
    REGISTERED,
    IN_DIAGNOSIS,
    IN_WORKSHOP,
    QUALITY_CONTROL,
    COMPLETED,
    CANCELLED
}
