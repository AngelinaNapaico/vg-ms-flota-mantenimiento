package pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Puerto de salida para orquestación con vg-ms-recursos.
 */
public interface ResourceClientPort {

    Mono<Void> disableResourceForMaintenance(String resourceId, String bearerToken);

    Mono<Void> enableResourceToAvailable(String resourceId, String bearerToken);

    Mono<Void> disableResourceForMaintenance(String resourceId);

    Mono<Void> enableResourceToAvailable(String resourceId);
}
