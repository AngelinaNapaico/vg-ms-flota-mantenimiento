package pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Vehicular maintenance types supported by the fleet service.
 * 100% in English according to GLOBAL-NAM-EN-001 and DDD standards.
 * External JSON contracts map to PRS cluster catalog values.
 */
public enum MaintenanceType {

    PREVENTIVE("PREVENTIVO"),
    CORRECTIVE("CORRECTIVO"),
    CORRECTIVE_ACCIDENT("CORRECTIVO_ACCIDENTE"),
    STATUTORY_INSPECTION("INSPECCION_REGLAMENTARIA");

    private final String externalCode;

    MaintenanceType(String externalCode) {
        this.externalCode = externalCode;
    }

    @JsonValue
    public String getExternalCode() {
        return externalCode;
    }

    @JsonCreator
    public static MaintenanceType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "PREVENTIVO", "PREVENTIVE" -> PREVENTIVE;
            case "CORRECTIVO", "CORRECTIVE" -> CORRECTIVE;
            case "CORRECTIVO_ACCIDENTE", "CORRECTIVE_ACCIDENT" -> CORRECTIVE_ACCIDENT;
            case "INSPECCION_REGLAMENTARIA", "STATUTORY_INSPECTION", "REGULATORY_INSPECTION" -> STATUTORY_INSPECTION;
            default -> {
                try {
                    yield MaintenanceType.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
}
