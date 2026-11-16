package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.SecurityContextPort;
import reactor.core.publisher.Mono;

/**
 * Secondary adapter implementing SecurityContextPort to provide security details
 * to the domain/application layer without leaking infrastructure dependencies.
 */
@Component
public class SecurityContextAdapter implements SecurityContextPort {

    @Override
    public Mono<String> getCurrentUsername() {
        return SecurityUtils.getCurrentUsername();
    }

    @Override
    public Mono<String> getCurrentEntityId() {
        return TenantScope.current()
                .map(TenantScope::entityId)
                .defaultIfEmpty("UNKNOWN");
    }
}
