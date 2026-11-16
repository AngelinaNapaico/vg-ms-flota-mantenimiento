package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.AuthClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security.SecurityUtils;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security.TenantScope;
import reactor.core.publisher.Mono;

/**
 * Secondary adapter implementing AuthClientPort to provide security details
 * to the domain/application layer without leaking infrastructure dependencies.
 */
@Component
public class AuthClientAdapter implements AuthClientPort {

    @Override
    public Mono<String> getCurrentUsername() {
        return SecurityUtils.getCurrentUsername();
    }

    @Override
    public Mono<String> getCurrentEntityId() {
        return TenantScope.current()
                .flatMap(scope -> scope.entityId() != null ? Mono.just(scope.entityId()) : Mono.empty());
    }
}
