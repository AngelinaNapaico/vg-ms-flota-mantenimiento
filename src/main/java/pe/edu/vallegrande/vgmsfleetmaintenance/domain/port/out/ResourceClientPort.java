package pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Outbound client port for orchestration with the external resources microservice (vg-ms-recursos).
 * Declares domain contracts for resource availability toggles and context ownership validation.
 */
public interface ResourceClientPort {

    Mono<Void> disableResourceForMaintenance(String resourceId, String bearerToken);

    Mono<Void> enableResourceToAvailable(String resourceId, String bearerToken);

    Mono<Void> disableResourceForMaintenance(String resourceId);

    Mono<Void> enableResourceToAvailable(String resourceId);

    Mono<Void> validateResourceBelongsToEntity(String resourceId, String entityId, String bearerToken);

    Mono<Void> validateResourceBelongsToEntity(String resourceId, String entityId);
}
