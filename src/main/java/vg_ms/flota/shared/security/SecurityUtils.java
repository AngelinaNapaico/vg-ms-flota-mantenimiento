package vg_ms.flota.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

public class SecurityUtils {

    private SecurityUtils() {}

    public static Mono<String> getCurrentJwtToken() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(auth -> {
                    Object credentials = auth.getCredentials();
                    return credentials != null ? credentials.toString() : "";
                })
                .defaultIfEmpty("");
    }

    public static Mono<String> getCurrentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .defaultIfEmpty("ANONYMOUS");
    }
}
