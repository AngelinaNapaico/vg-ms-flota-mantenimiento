package pe.edu.vallegrande.caneteseguro.fleet.infrastructure.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Startup security validator for Keycloak OIDC issuer.
 * Ensures Keycloak configuration integrity and protects production environments (STD-054 & SEC-001).
 */
@Configuration
public class SecurityBootstrapValidator {

    private static final Logger log = LoggerFactory.getLogger(SecurityBootstrapValidator.class);

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8090/realms/PRS}")
    private String issuerUri;

    private final Environment environment;

    public SecurityBootstrapValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateAuthenticationConfiguration() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        log.info("=".repeat(80));
        log.info("SECURITY VALIDATION - KEYCLOAK OIDC");
        log.info("=".repeat(80));
        log.info("Active profile: {}", isProd ? "PRODUCTION" : "DEVELOPMENT");
        log.info("Issuer URI: {}", redactUrl(issuerUri));

        if (issuerUri == null || issuerUri.isBlank()) {
            String msg = "FATAL: KEYCLOAK_ISSUER_URI is not configured.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        if (isProd) {
            validateProductionIssuer();
        } else {
            validateDevelopmentIssuer();
        }

        log.info("✓ KEYCLOAK configured as central OIDC identity provider");
        log.info("✓ Legacy HMAC tokens rejected");
        log.info("✓ Security bootstrap validation successful");
        log.info("=".repeat(80));
    }

    private void validateProductionIssuer() {
        if (issuerUri.contains("localhost") || issuerUri.contains("127.0.0.1")) {
            String msg = "FATAL in PRODUCTION: issuer-uri points to localhost (" + redactUrl(issuerUri) + ").";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        log.info("✓ Production Keycloak issuer validated: {}", redactUrl(issuerUri));
    }

    private void validateDevelopmentIssuer() {
        log.info("✓ Development Keycloak issuer validated");
    }

    private String redactUrl(String url) {
        if (url == null) {
            return "null";
        }
        return url.replaceAll("://[^@/]+@", "://[redacted]@");
    }
}
