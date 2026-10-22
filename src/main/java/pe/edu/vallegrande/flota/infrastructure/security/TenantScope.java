package pe.edu.vallegrande.flota.infrastructure.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Alcance multitenant del usuario autenticado (claims {@code mid}/{@code eid} del JWT).
 * Regla de negocio para Flota y Mantenimiento:
 * - SUPER_ADMIN ve y gestiona todas las órdenes y flotas.
 * - Con {@code eid} (Entidad): ve y gestiona exclusivamente las órdenes pertenecientes a su entidad.
 * - Usuarios sin {@code eid} (incluyendo solo municipalidad): no intervienen en el mantenimiento vehicular.
 * Copiado igual en cada micro (no hay librería compartida): mantener en sincronía.
 */
public record TenantScope(boolean all, String municipalidadId, String entidadId, Set<String> roles) {

    public static Mono<TenantScope> current() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(TenantScope::from)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")));
    }

    public static TenantScope from(Authentication auth) {
        if (auth == null) {
            return new TenantScope(false, null, null, Set.of());
        }
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toUnmodifiableSet());
        Map<String, Object> claims = auth instanceof JwtAuthenticationToken jwt ? jwt.getTokenAttributes() : Map.of();
        return new TenantScope(roles.contains("SUPER_ADMIN"), text(claims.get("mid")), text(claims.get("eid")), roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Lectura de orden de mantenimiento perteneciente a {@code eid}.
     * Solo SUPER_ADMIN o personal con el mismo {@code entidadId}.
     */
    public boolean canRead(String eid) {
        if (all) {
            return true;
        }
        return entidadId != null && entidadId.equals(eid);
    }

    /**
     * Escritura / Gestión de órdenes: solo personal de entidad (o SUPER_ADMIN), excepto rol ANALISTA.
     */
    public boolean canWrite() {
        return all || (entidadId != null && !hasRole("ANALISTA"));
    }

    public TenantScope requireWriter() {
        if (!canWrite()) {
            throw forbidden();
        }
        return this;
    }

    public static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso restringido: La flota vehicular solo es gestionada por la entidad perteneciente");
    }

    private static String text(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }
}
