package vg_ms.flota.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        String preferredUsername = claims.get("preferred_username", String.class);
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaims(token);

        // 1. Roles en claim directo "roles" (vg-ms-auth)
        List<String> roles = claims.get("roles", List.class);
        if (roles != null && !roles.isEmpty()) {
            return roles;
        }

        // 2. Roles de Keycloak en realm_access.roles
        Object realmAccessObj = claims.get("realm_access");
        if (realmAccessObj instanceof java.util.Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List<?> list) {
                return list.stream().map(Object::toString).toList();
            }
        }

        return List.of();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        String uid = claims.get("uid", String.class);
        return uid != null ? uid : claims.getSubject();
    }

    public String getEntidadIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("eid", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Token JWT no válido o no firmado con la clave compartida: {}", e.getMessage());
            return false;
        }
    }
}
