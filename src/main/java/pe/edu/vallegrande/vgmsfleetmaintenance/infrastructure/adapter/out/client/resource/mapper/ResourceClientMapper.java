package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.mapper;

import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.dto.OperationalResourceDTO;

/**
 * Mapper for external resource client DTOs.
 * Prepares payload structures for interaction with the resources microservice.
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
