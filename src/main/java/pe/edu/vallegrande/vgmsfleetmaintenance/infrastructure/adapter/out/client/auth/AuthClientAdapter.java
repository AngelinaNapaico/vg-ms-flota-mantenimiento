package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.AuthClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.dto.AuthUserDTO;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.mapper.AuthUserMapper;
import reactor.core.publisher.Mono;

/**
 * Secondary adapter implementing AuthClientPort to provide normalized security identity
 * and tenant access metadata to the application layer.
 */
@Component
public class AuthClientAdapter implements AuthClientPort {

    @Override
    public Mono<String> getCurrentUsername() {
        return resolveAuthenticatedUser()
                .map(AuthUserDTO::username)
                .defaultIfEmpty("ANONYMOUS");
    }

    @Override
    public Mono<String> getCurrentEntityId() {
        return resolveAuthenticatedUser()
                .flatMap(user -> user.entityId() != null ? Mono.just(user.entityId()) : Mono.empty());
    }

    @Override
    public Mono<Boolean> hasGlobalAccess() {
        return resolveAuthenticatedUser()
                .map(AuthUserDTO::globalAccess)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> hasEntityWideAccess() {
        return resolveAuthenticatedUser()
                .map(AuthUserDTO::entityWideAccess)
                .defaultIfEmpty(false);
    }

    private Mono<AuthUserDTO> resolveAuthenticatedUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(AuthUserMapper::fromAuthentication);
    }
}
