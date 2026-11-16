package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

/**
 * Reactive JWT decoder configuration for Keycloak OIDC token verification.
 * Enforces cryptographic signature verification and maps token validation errors
 * to BadCredentialsException for consistent authentication rejection.
 */
@Configuration
public class JwtDecoderConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:${KEYCLOAK_JWK_SET_URI:http://localhost:8090/realms/PRS/protocol/openid-connect/certs}}")
    private String jwkSetUri;

    @Bean
    @Profile("prod")
    public ReactiveJwtDecoder jwtDecoderProduction() {
        validateProductionIssuer();
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuerUri);
        decoder.setJwtValidator(validator);
        return token -> decoder.decode(token)
                .onErrorMap(Exception.class,
                        ex -> new BadCredentialsException("Invalid or unverifiable JWT token in production", ex));
    }

    @Bean
    @Profile("!prod")
    public ReactiveJwtDecoder jwtDecoderDevelopment() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuerUri);
        decoder.setJwtValidator(validator);
        return token -> decoder.decode(token)
                .onErrorMap(Exception.class,
                        ex -> new BadCredentialsException("Invalid or unverifiable JWT token", ex));
    }

    public void validateProductionIssuer() {
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new IllegalStateException(
                    "In production KEYCLOAK_ISSUER_URI is mandatory. " +
                            "Development tokens, legacy HMAC, or foreign providers are rejected.");
        }

        if (issuerUri.contains("localhost") || issuerUri.contains("127.0.0.1")) {
            throw new IllegalStateException(
                    "Production profile does not allow local issuer (localhost/127.0.0.1). " +
                            "KEYCLOAK_ISSUER_URI must target production Keycloak instance with HTTPS.");
        }
    }
}
