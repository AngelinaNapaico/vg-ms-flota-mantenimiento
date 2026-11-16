package pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Outbound port for querying the current authenticated security context.
 * Decouples the application layer from security infrastructure.
 */
public interface SecurityContextPort {

    /**
     * Resolves the username or principal name of the current authenticated user.
     */
    Mono<String> getCurrentUsername();

    /**
     * Resolves the tenant (entity) identifier of the current authenticated user.
     */
    Mono<String> getCurrentEntityId();
}
