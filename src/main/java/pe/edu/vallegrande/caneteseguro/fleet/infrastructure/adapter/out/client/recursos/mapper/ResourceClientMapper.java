package pe.edu.vallegrande.caneteseguro.fleet.infrastructure.adapter.out.client.recursos.mapper;

import pe.edu.vallegrande.caneteseguro.fleet.infrastructure.adapter.out.client.recursos.dto.OperationalResourceDTO;

/**
 * Mapper for external resources client contracts (STD-040 & STD-087).
 * Maps between external HTTP responses from vg-ms-recursos and internal representations.
 */
public final class ResourceClientMapper {

    private ResourceClientMapper() {}

    /**
     * Extracts normalized plate or internal code from an operational resource response.
     */
    public static String extractPlateOrCode(OperationalResourceDTO dto) {
        if (dto == null) {
            return null;
        }
        if (dto.getLicensePlate() != null && !dto.getLicensePlate().isBlank()) {
            return dto.getLicensePlate();
        }
        return dto.getInternalCode();
    }
}
