package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reactive security configuration and Keycloak JWT role mapping.
 * Enforces normalized JSON error responses for 401 Unauthorized and 403 Forbidden
 * without exposing internal security details, and restricts API documentation
 * and non-essential actuator endpoints outside production.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private static final String[] HEALTH_PATHS = {
            "/actuator/health"
    };

    private static final String[] NON_PROD_PUBLIC_PATHS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
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
            ReactiveJwtDecoder jwtDecoder,
            Environment environment) {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> {
                    exchanges.pathMatchers(HEALTH_PATHS).permitAll();
                    if (!isProd) {
                        exchanges.pathMatchers(NON_PROD_PUBLIC_PATHS).permitAll();
                    }
                    exchanges
                            .pathMatchers(HttpMethod.GET, "/api/v1/maintenance-orders/**").hasAnyRole(READ_ROLES)
                            .pathMatchers(HttpMethod.POST, "/api/v1/maintenance-orders/**").hasAnyRole(WRITE_ROLES)
                            .pathMatchers(HttpMethod.PUT, "/api/v1/maintenance-orders/**").hasAnyRole(WRITE_ROLES)
                            .pathMatchers(HttpMethod.PATCH, "/api/v1/maintenance-orders/**").hasAnyRole(WRITE_ROLES)
                            .pathMatchers(HttpMethod.DELETE, "/api/v1/maintenance-orders/**").hasAnyRole(WRITE_ROLES)
                            .anyExchange().denyAll();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, ex) -> writeErrorResponse(
                                exchange,
                                HttpStatus.UNAUTHORIZED,
                                "Unauthorized",
                                "Authentication required or invalid credentials"))
                        .accessDeniedHandler((exchange, ex) -> writeErrorResponse(
                                exchange,
                                HttpStatus.FORBIDDEN,
                                "Forbidden",
                                "Access denied: insufficient permissions")))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint((exchange, ex) -> writeErrorResponse(
                                exchange,
                                HttpStatus.UNAUTHORIZED,
                                "Unauthorized",
                                "Authentication required or invalid credentials"))
                        .accessDeniedHandler((exchange, ex) -> writeErrorResponse(
                                exchange,
                                HttpStatus.FORBIDDEN,
                                "Forbidden",
                                "Access denied: insufficient permissions"))
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    private Mono<Void> writeErrorResponse(
            ServerWebExchange exchange,
            HttpStatus status,
            String error,
            String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String payload = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                Instant.now().toString(),
                status.value(),
                error,
                message);
        DataBuffer buffer = response.bufferFactory().wrap(payload.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
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
