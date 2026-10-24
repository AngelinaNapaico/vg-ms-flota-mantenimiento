package pe.edu.vallegrande.caneteseguro.fleet.domain.vo;

/**
 * Niveles de prioridad operativa de una orden de mantenimiento.
 * Convención en inglés según estándar DatabaseStorage (NOM-03).
 */
public enum OrderPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
