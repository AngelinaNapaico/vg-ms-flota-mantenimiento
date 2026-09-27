package vg_ms.flota.shared.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String headerAuth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            String token = headerAuth.substring(7);

            if (jwtUtils.validateToken(token)) {
                String username = jwtUtils.getUsernameFromToken(token);
                List<String> roles = jwtUtils.getRolesFromToken(token);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (roles != null) {
                    for (String r : roles) {
                        if (r != null && !r.isBlank()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()));
                        }
                    }
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, token, authorities);

                SecurityContext context = new SecurityContextImpl(authentication);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
            }
        }

        return chain.filter(exchange);
    }
}
