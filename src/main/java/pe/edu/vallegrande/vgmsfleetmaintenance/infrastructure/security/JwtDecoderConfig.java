package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;

/**
 * Explicit JWT decoder configuration ensuring ONLY Keycloak OIDC is accepted.
 * Legacy HMAC tokens or unauthorized providers are strictly rejected.
 */
@Configuration
public class JwtDecoderConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8090/realms/PRS}")
    private String issuerUri;

    @Bean
    @Profile("prod")
    public ReactiveJwtDecoder jwtDecoderProduction() {
        validateProductionIssuer();
        return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    @Profile("!prod")
    public ReactiveJwtDecoder jwtDecoderDevelopment() {
        return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
    }

    @PostConstruct
    @Profile("prod")
    public void validateProductionIssuer() {
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new IllegalStateException(
                    "In production KEYCLOAK_ISSUER_URI is mandatory. " +
                    "Development tokens, legacy HMAC, or foreign providers are rejected."
            );
        }

        if (issuerUri.contains("localhost") || issuerUri.contains("127.0.0.1")) {
            throw new IllegalStateException(
                    "Production profile does not allow local issuer (localhost/127.0.0.1). " +
                    "KEYCLOAK_ISSUER_URI must target production Keycloak instance with HTTPS."
            );
        }
    }
}
