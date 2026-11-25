package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.mapper;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.dto.AuthUserDTO;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper converting Spring Security Authentication principals, JWT claims,
 * and granted authorities into a normalized AuthUserDTO.
 */
public final class AuthUserMapper {

    private static final String ANONYMOUS_USER = "ANONYMOUS";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private AuthUserMapper() {
    }

    public static AuthUserDTO fromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthUserDTO(ANONYMOUS_USER, null, false);
        }

        Map<String, Object> claims = extractClaims(authentication);
        String username = resolveUsername(claims, authentication);
        String entityId = normalizeClaim(claims.get("eid"));
        Set<String> normalizedRoles = extractNormalizedRoles(authentication);
        boolean globalAccess = normalizedRoles.contains(SUPER_ADMIN_ROLE);

        return new AuthUserDTO(username, entityId, globalAccess);
    }

    private static Map<String, Object> extractClaims(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            return jwtToken.getTokenAttributes();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaims();
        }
        return Map.of();
    }

    private static String resolveUsername(Map<String, Object> claims, Authentication authentication) {
        String dni = normalizeClaim(claims.get("dni"));
        if (dni != null) {
            return dni;
        }
        String preferredUsername = normalizeClaim(claims.get("preferred_username"));
        if (preferredUsername != null) {
            return preferredUsername;
        }
        String subject = normalizeClaim(claims.get("sub"));
        if (subject != null) {
            return subject;
        }
        String principalName = normalizeClaim(authentication.getName());
        return principalName != null ? principalName : ANONYMOUS_USER;
    }

    private static Set<String> extractNormalizedRoles(Authentication authentication) {
        if (authentication.getAuthorities() == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && !authority.isBlank())
                .map(authority -> authority.startsWith(ROLE_PREFIX)
                        ? authority.substring(ROLE_PREFIX.length())
                        : authority)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalizeClaim(Object value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.toString().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
