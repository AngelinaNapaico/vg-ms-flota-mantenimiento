package pe.edu.vallegrande.caneteseguro.flota.domain.vo;

/**
 * Tipos de mantenimiento vehicular soportados.
 * Convención en inglés según estándar DatabaseStorage (NOM-03).
 */
public enum MaintenanceType {
    PREVENTIVE,
    CORRECTIVE,
    CORRECTIVE_ACCIDENT,
    REGULATORY_INSPECTION
}
