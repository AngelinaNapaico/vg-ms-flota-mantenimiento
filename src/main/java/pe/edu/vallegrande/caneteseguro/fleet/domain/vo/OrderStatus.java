package pe.edu.vallegrande.caneteseguro.fleet.domain.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Operational lifecycle states of a fleet maintenance order.
 * 100% in English according to GLOBAL-NAM-EN-001 and DDD standards.
 * External JSON contracts map to PRS cluster catalog values.
 */
public enum OrderStatus {

    REGISTERED("REGISTRADA"),
    IN_DIAGNOSIS("EN_DIAGNOSTICO"),
    IN_WORKSHOP("EN_TALLER"),
    IN_QUALITY_CONTROL("EN_CONTROL_CALIDAD"),
    COMPLETED("FINALIZADA"),
    CANCELLED("CANCELADA");

    private final String externalCode;

    OrderStatus(String externalCode) {
        this.externalCode = externalCode;
    }

    @JsonValue
    public String getExternalCode() {
        return externalCode;
    }

    @JsonCreator
    public static OrderStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "REGISTRADA", "REGISTERED" -> REGISTERED;
            case "EN_DIAGNOSTICO", "IN_DIAGNOSIS" -> IN_DIAGNOSIS;
            case "EN_TALLER", "IN_WORKSHOP", "IN_REPAIR" -> IN_WORKSHOP;
            case "EN_CONTROL_CALIDAD", "IN_QUALITY_CONTROL", "QUALITY_CONTROL" -> IN_QUALITY_CONTROL;
            case "FINALIZADA", "COMPLETED" -> COMPLETED;
            case "CANCELADA", "CANCELLED" -> CANCELLED;
            default -> {
                try {
                    yield OrderStatus.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
}
