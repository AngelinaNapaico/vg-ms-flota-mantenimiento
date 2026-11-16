package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.mapper;

import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.dto.OperationalResourceDTO;

/**
 * Mapper for external resource client DTOs.
 * Complies with STD-040, STD-061, STD-087:
 * - Resides in infrastructure/adapter/out/client/resource/mapper/
 * - Connects OperationalResourceDTO with the outbound HTTP adapter workflow.
 */
public final class ResourceClientMapper {

    private ResourceClientMapper() {}

    /**
     * Builds an OperationalResourceDTO request representation for status updates.
     */
    public static OperationalResourceDTO toDTO(String resourceId, String availability) {
        if (resourceId == null) {
            return null;
        }
        return OperationalResourceDTO.builder()
                .id(resourceId)
                .availability(availability)
                .build();
    }
}
