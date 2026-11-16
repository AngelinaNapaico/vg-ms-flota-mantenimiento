package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.mapper;

import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.dto.AuthUserDTO;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security.TenantScope;

/**
 * Mapper for converting security context and authentication scopes into client DTOs.
 */
public final class AuthUserMapper {

    private AuthUserMapper() {}

    /**
     * Maps an authenticated username and tenant scope into an AuthUserDTO.
     */
    public static AuthUserDTO toDTO(String username, TenantScope scope) {
        if (scope == null) {
            return AuthUserDTO.builder()
                    .username(username != null ? username : "ANONYMOUS")
                    .build();
        }

        return AuthUserDTO.builder()
                .username(username != null ? username : "ANONYMOUS")
                .entityId(scope.entityId())
                .municipalityId(scope.municipalityId())
                .roles(scope.roles())
                .build();
    }
}
