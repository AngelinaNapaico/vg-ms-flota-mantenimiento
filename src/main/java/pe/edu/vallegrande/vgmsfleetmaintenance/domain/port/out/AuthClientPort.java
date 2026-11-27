package pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Outbound client port providing authenticated user identity and tenant context
 * to the domain and application layers without leaking infrastructure classes.
 */
public interface AuthClientPort {

    Mono<String> getCurrentUsername();

    Mono<String> getCurrentEntityId();

    Mono<Boolean> hasGlobalAccess();

    Mono<Boolean> hasEntityWideAccess();
}
