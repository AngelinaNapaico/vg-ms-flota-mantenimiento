package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.mapper;

import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.dto.OperationalResourceDTO;

/**
 * Mapper for external resource client DTOs.
 * Performs bidirectional domain-to-contract transformations and status resolution.
 */
public final class ResourceClientMapper {

    private ResourceClientMapper() {}

    /**
     * Extracts the resource payload from either a wrapped API response ({@code data})
     * or a direct flat JSON payload returned by vg-ms-recursos.
     */
    public static OperationalResourceDTO unwrap(OperationalResourceDTO response) {
        if (response == null) {
            return null;
        }
        if (response.getData() != null) {
            return response.getData();
        }
        if (response.getId() == null && response.getEntityId() == null && response.getAvailability() == null) {
            return null;
        }
        return response;
    }

    /**
     * Builds an OperationalResourceDTO request representation for status updates
     * with domain status normalization and business status details.
     */
    public static OperationalResourceDTO toDTO(String resourceId, String availability) {
        if (resourceId == null || resourceId.isBlank()) {
            return null;
        }

        String normalizedAvailability = normalizeAvailability(availability);
        String statusDetail = resolveStatusDetail(normalizedAvailability);

        return OperationalResourceDTO.builder()
                .id(resourceId.trim())
                .availability(normalizedAvailability)
                .statusDetail(statusDetail)
                .build();
    }

    private static String normalizeAvailability(String availability) {
        if (availability == null || availability.isBlank()) {
            return "OUT_OF_SERVICE";
        }
        String upper = availability.trim().toUpperCase();
        return switch (upper) {
            case "MAINTENANCE", "UNDER_MAINTENANCE", "EN_MANTENIMIENTO" -> "UNDER_MAINTENANCE";
            case "AVAILABLE", "DISPONIBLE" -> "AVAILABLE";
            default -> upper;
        };
    }

    private static String resolveStatusDetail(String normalizedAvailability) {
        return "UNDER_MAINTENANCE".equals(normalizedAvailability)
                ? "Preventive or corrective maintenance"
                : "Operational";
    }
}
