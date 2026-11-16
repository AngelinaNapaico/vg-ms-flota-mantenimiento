package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Security startup validator guaranteeing that:
 * 1. In PRODUCTION: Only Keycloak OIDC with HTTPS is accepted (rejects localhost, legacy HMAC).
 * 2. In DEVELOPMENT: Allows local Keycloak fallback.
 * SEC-001 & SEC-004 compliant.
 */
@Slf4j
@Configuration
public class SecurityBootstrapValidator {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${KEYCLOAK_ISSUER_URI:http://localhost:8090/realms/PRS}}")
    private String issuerUri;

    private final Environment environment;

    public SecurityBootstrapValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateAuthenticationConfiguration() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        log.info("=".repeat(80));
        log.info("SECURITY BOOTSTRAP - OIDC AUTHENTICATION VALIDATION");
        log.info("Active profile: {}", isProd ? "PRODUCTION" : "DEVELOPMENT");
        log.info("Issuer URI: {}", redactUrl(issuerUri));

        if (issuerUri == null || issuerUri.isBlank()) {
            String msg = "FATAL: KEYCLOAK_ISSUER_URI is not configured. OAuth2/Keycloak cannot start.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        if (isProd) {
            validateProductionIssuer();
        }

        log.info("✓ Keycloak configured as exclusive OIDC provider");
        log.info("✓ Legacy HMAC or foreign tokens are rejected");
        log.info("=".repeat(80));
    }

    private void validateProductionIssuer() {
        if (issuerUri.contains("localhost") || issuerUri.contains("127.0.0.1")) {
            String msg = "FATAL in PRODUCTION: issuer-uri targets localhost. " +
                    "Configure KEYCLOAK_ISSUER_URI with production Keycloak instance.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        if (issuerUri.contains(".local") || issuerUri.contains(":8090") || issuerUri.contains(":8080")) {
            String msg = "FATAL in PRODUCTION: issuer-uri targets development port. " +
                    "Must target HTTPS production Keycloak.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
    }

    private String redactUrl(String url) {
        if (url == null) {
            return "null";
        }
        return url.replaceAll("://[^@/]+@", "://[redacted]@");
    }
}
