package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.AuthClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.dto.AuthUserDTO;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.mapper.AuthUserMapper;
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
        return getAuthenticatedUser().map(AuthUserDTO::getUsername);
    }

    @Override
    public Mono<String> getCurrentEntityId() {
        return getAuthenticatedUser()
                .flatMap(user -> user.getEntityId() != null ? Mono.just(user.getEntityId()) : Mono.empty());
    }

    public Mono<AuthUserDTO> getAuthenticatedUser() {
        return SecurityUtils.getCurrentUsername()
                .defaultIfEmpty("ANONYMOUS")
                .flatMap(username -> TenantScope.current()
                        .map(scope -> AuthUserMapper.toDTO(username, scope))
                        .defaultIfEmpty(AuthUserMapper.toDTO(username, null)));
    }
}
