package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

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
 * Multitenant scope of the authenticated user (JWT claims
 * {@code mid}/{@code eid}).
 * 100% in English according to GLOBAL-NAM-EN-001.
 * Business rules:
 * - SUPER_ADMIN/ADMIN has global view and management of all orders and fleets.
 * - MUNICIPALIDAD_ADMIN has global read/audit visibility to supervise fleets
 * and export reports.
 * - With {@code eid} (Entity): views and manages exclusively orders belonging
 * to their entity.
 * - Users without {@code eid} and not admin/super_admin cannot create or modify
 * maintenance orders.
 */
public record TenantScope(boolean all, String municipalityId, String entityId, Set<String> roles) {

    public static Mono<TenantScope> current() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(TenantScope::from)
                .switchIfEmpty(
                        Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated")));
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
        boolean isGlobalAdmin = roles.contains("SUPER_ADMIN") || roles.contains("ADMIN");
        return new TenantScope(isGlobalAdmin, text(claims.get("mid")), text(claims.get("eid")), roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Read access for maintenance order belonging to {@code eid}.
     * Restricted to documents belonging to the user's entityId, or cross-tenant
     * access exclusively for global administrators without tenant binding.
     */
    public boolean canRead(String eid) {
        if (entityId != null) {
            return entityId.equals(eid);
        }
        return all;
    }

    /**
     * Write/Management of orders: only entity operational personnel (or
     * SUPER_ADMIN/ADMIN).
     * MUNICIPALIDAD_ADMIN and ANALISTA are strictly read-only supervisory roles.
     */
    public boolean canWrite() {
        if (all) {
            return true;
        }
        if (hasRole("MUNICIPALIDAD_ADMIN") || hasRole("ANALISTA")) {
            return false;
        }
        return entityId != null;
    }

    public TenantScope requireWriter() {
        if (!canWrite()) {
            throw forbidden();
        }
        return this;
    }

    public static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied: Fleet vehicles are only managed by the owning entity");
    }

    private static String text(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }
}
