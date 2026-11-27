package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.dto.AuthUserDTO;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AuthUserMapper - Authentication & JWT Claim Transformation")
class AuthUserMapperTest {

    @Test
    @DisplayName("Should prioritize dni claim, trim entityId, and map non-global operator role")
    void shouldTransformOperatorJwtAuthentication() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .subject("sub-001")
                .claim("dni", " 44556677 ")
                .claim("preferred_username", "operator-a")
                .claim("eid", " ent-001 ")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_OPERADOR_ENTIDAD")));

        AuthUserDTO result = AuthUserMapper.fromAuthentication(auth);

        assertEquals("44556677", result.username());
        assertEquals("ent-001", result.entityId());
        assertFalse(result.globalAccess());
        assertFalse(result.entityWideAccess());
    }

    @Test
    @DisplayName("Should derive globalAccess and entityWideAccess true for SUPER_ADMIN role")
    void shouldTransformSuperAdminJwtAuthentication() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .subject("sub-admin")
                .claim("preferred_username", "super-admin")
                .claim("eid", "   ")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        AuthUserDTO result = AuthUserMapper.fromAuthentication(auth);

        assertEquals("super-admin", result.username());
        assertNull(result.entityId());
        assertTrue(result.globalAccess());
        assertTrue(result.entityWideAccess());
    }

    @Test
    @DisplayName("Should derive entityWideAccess true and globalAccess false for ENTIDAD_ADMIN role")
    void shouldGrantEntityWideAccessToEntityAdmin() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .subject("sub-entity-admin")
                .claim("preferred_username", "entity-admin")
                .claim("eid", "ent-002")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_ENTIDAD_ADMIN")));

        AuthUserDTO result = AuthUserMapper.fromAuthentication(auth);

        assertEquals("entity-admin", result.username());
        assertEquals("ent-002", result.entityId());
        assertFalse(result.globalAccess());
        assertTrue(result.entityWideAccess());
    }

    @Test
    @DisplayName("Should return anonymous DTO when authentication is null")
    void shouldReturnAnonymousForNullAuthentication() {
        AuthUserDTO result = AuthUserMapper.fromAuthentication(null);

        assertEquals("ANONYMOUS", result.username());
        assertNull(result.entityId());
        assertFalse(result.globalAccess());
        assertFalse(result.entityWideAccess());
    }
}
