package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.AuthClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security.SecurityUtils;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security.TenantScope;
import reactor.core.publisher.Mono;

/**
 * Secondary adapter implementing AuthClientPort to provide security details
 * to the domain and application layers without leaking infrastructure dependencies.
 */
@Component
public class AuthClientAdapter implements AuthClientPort {

    @Override
    public Mono<String> getCurrentUsername() {
        return SecurityUtils.getCurrentUsername()
                .defaultIfEmpty("ANONYMOUS");
    }

    @Override
    public Mono<String> getCurrentEntityId() {
        return resolveCurrentScope()
                .flatMap(scope -> scope.entityId() != null ? Mono.just(scope.entityId()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> hasGlobalAccess() {
        return resolveCurrentScope()
                .map(TenantScope::all)
                .defaultIfEmpty(false);
    }

    private Mono<TenantScope> resolveCurrentScope() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(TenantScope::from);
    }
}
