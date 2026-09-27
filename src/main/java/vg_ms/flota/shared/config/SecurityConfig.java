package vg_ms.flota.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .authorizeExchange(exchanges -> exchanges
                        // Endpoints públicos (Swagger, OpenAPI, Actuator)
                        .pathMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        // Endpoints internos llamados entre microservicios
                        .pathMatchers("/internal/**").permitAll()
                        // Todo lo demás requiere autenticación; el rol específico lo define @PreAuthorize
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .build();
    }

    /**
     * Extrae y mapea los roles tanto de tokens emitidos por Keycloak como por vg-ms-auth.
     */
    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = new ArrayList<>();

            // 1. Claim directo "roles" (usado en realm PRS o vg-ms-auth)
            List<String> flatRoles = jwt.getClaimAsStringList("roles");
            if (flatRoles != null) {
                roles.addAll(flatRoles);
            }

            // 2. Roles de Keycloak a nivel de Realm: realm_access.roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof List<?> realmRoles) {
                realmRoles.stream()
                        .map(Object::toString)
                        .forEach(roles::add);
            }

            // 3. Roles de clientes Keycloak: resource_access.*.roles
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                resourceAccess.values().forEach(clientAccess -> {
                    if (clientAccess instanceof Map<?, ?> clientMap
                            && clientMap.get("roles") instanceof List<?> clientRoles) {
                        clientRoles.stream()
                                .map(Object::toString)
                                .forEach(roles::add);
                    }
                });
            }

            // Agrega prefijo ROLE_ requerido por hasRole / hasAnyRole
            return roles.stream()
                    .filter(r -> r != null && !r.isBlank())
                    .distinct()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });

        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
