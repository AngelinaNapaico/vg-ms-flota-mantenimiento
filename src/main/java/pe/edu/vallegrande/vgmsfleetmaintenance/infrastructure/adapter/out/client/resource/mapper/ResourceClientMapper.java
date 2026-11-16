package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.mapper;

import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.dto.OperationalResourceDTO;

/**
 * Mapper for external resource client DTOs.
 * Performs bidirectional domain-to-contract transformations and status resolution.
 */
public final class ResourceClientMapper {

    private ResourceClientMapper() {}

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
            return "NO_DISPONIBLE";
        }
        String upper = availability.trim().toUpperCase();
        return switch (upper) {
            case "MAINTENANCE", "EN_MANTENIMIENTO" -> "EN_MANTENIMIENTO";
            case "AVAILABLE", "DISPONIBLE" -> "DISPONIBLE";
            default -> upper;
        };
    }

    private static String resolveStatusDetail(String normalizedAvailability) {
        return "EN_MANTENIMIENTO".equals(normalizedAvailability)
                ? "Mantenimiento preventivo/correctivo"
                : "Operativo";
    }
}
