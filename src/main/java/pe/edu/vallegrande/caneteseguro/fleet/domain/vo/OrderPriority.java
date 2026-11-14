package pe.edu.vallegrande.caneteseguro.fleet.domain.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Operational priority levels for fleet maintenance orders.
 * 100% in English according to GLOBAL-NAM-EN-001 and DDD standards.
 * External JSON contracts map to PRS cluster catalog values.
 */
public enum OrderPriority {

    LOW("BAJA"),
    MEDIUM("MEDIA"),
    HIGH("ALTA"),
    CRITICAL("CRITICA");

    private final String externalCode;

    OrderPriority(String externalCode) {
        this.externalCode = externalCode;
    }

    @JsonValue
    public String getExternalCode() {
        return externalCode;
    }

    @JsonCreator
    public static OrderPriority fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "BAJA", "LOW" -> LOW;
            case "MEDIA", "MEDIUM" -> MEDIUM;
            case "ALTA", "HIGH" -> HIGH;
            case "CRITICA", "CRITICAL" -> CRITICAL;
            default -> {
                try {
                    yield OrderPriority.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
}
