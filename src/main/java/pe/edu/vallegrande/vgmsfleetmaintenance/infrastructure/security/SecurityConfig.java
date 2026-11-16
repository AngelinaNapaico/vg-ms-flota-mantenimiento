package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reactive security configuration and Keycloak JWT role mapping.
 * Enforces authentication entry points returning 401 Unauthorized for invalid
 * tokens
 * and authorization checks returning 403 Forbidden for insufficient
 * permissions.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/actuator/health",
            "/actuator/info"
    };

    private static final String[] READ_ROLES = {
            "SUPER_ADMIN", "ADMIN", "MUNICIPALIDAD_ADMIN", "ENTIDAD_ADMIN",
            "OPERADOR_ENTIDAD", "CIUDADANO", "ANALISTA", "ANALISTA_ENTIDAD"
    };

    private static final String[] WRITE_ROLES = {
            "SUPER_ADMIN", "ADMIN", "ENTIDAD_ADMIN", "OPERADOR_ENTIDAD"
    };

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/maintenance-orders/**").hasAnyRole(READ_ROLES)
                        .pathMatchers(HttpMethod.POST, "/api/v1/maintenance-orders/**").hasAnyRole(WRITE_ROLES)
                        .pathMatchers(HttpMethod.PUT, "/api/v1/maintenance-orders/**").hasAnyRole(WRITE_ROLES)
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/maintenance-orders/**").hasAnyRole(WRITE_ROLES)
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/maintenance-orders/**").hasAnyRole(WRITE_ROLES)
                        .anyExchange().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = new ArrayList<>();

            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                Object r = realmAccess.get("roles");
                if (r instanceof List<?> list) {
                    list.stream().filter(String.class::isInstance).map(String.class::cast).forEach(roles::add);
                }
            }

            List<String> directRoles = jwt.getClaimAsStringList("roles");
            if (directRoles != null) {
                roles.addAll(directRoles);
            }
            List<String> authRoles = jwt.getClaimAsStringList("authorities");
            if (authRoles != null) {
                roles.addAll(authRoles);
            }

            return roles.stream()
                    .distinct()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });

        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
