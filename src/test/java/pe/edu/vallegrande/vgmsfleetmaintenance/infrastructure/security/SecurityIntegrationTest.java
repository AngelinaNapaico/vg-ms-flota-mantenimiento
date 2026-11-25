package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.AuditLogRepository;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.MaintenanceOrderRepository;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Authentication Integration Tests.
 * Uses a test ReactiveJwtDecoder configured with the matching RSA public key
 * so expiration and issuer checks are verified independently of signature validation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(SecurityIntegrationTest.TestJwtDecoderConfig.class)
@DisplayName("SecurityIntegrationTest - Endpoint Protection & JWT Verification")
class SecurityIntegrationTest {

    @TestConfiguration
    static class TestJwtDecoderConfig {
        @Bean
        @Primary
        ReactiveJwtDecoder testReactiveJwtDecoder() {
            NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                    .withPublicKey(TestJwtTokens.TEST_PUBLIC_KEY)
                    .build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TestJwtTokens.EXPECTED_ISSUER));
            return token -> decoder.decode(token)
                    .onErrorMap(Exception.class,
                            ex -> new BadCredentialsException("Invalid or unverifiable JWT token", ex));
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MaintenanceOrderRepository repository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private ResourceClientPort resourceClientPort;

    @MockBean
    private ReactiveMongoTemplate mongoTemplate;

    private static final String SAMPLE_CREATE_PAYLOAD = """
        {
            "resourceId": "65f1a2b3c4d5e6f7a8b9c0d1",
            "resourcePlate": "EGA-123",
            "entityId": "65f1a2b3c4d5e6f7a8b9c0e2",
            "maintenanceType": "PREVENTIVE",
            "priority": "HIGH",
            "failureReason": "Preventive maintenance check",
            "reportedMileage": 45000
        }
    """;

    @BeforeEach
    void setupRepositoryDefaults() {
        when(repository.findByEntityId(anyString())).thenReturn(Flux.empty());
    }

    @Test
    @DisplayName("Should return 200 OK for GET with valid RSA-signed token matching expected issuer")
    void shouldReturn200WithValidSignedToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.VALID_TOKEN)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Should return 401 with normalized JSON payload for GET without token")
    void shouldReturn401WhenListingWithoutToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Unauthorized")
                .jsonPath("$.message").isEqualTo("Authentication required or invalid credentials");
    }

    @Test
    @DisplayName("Should return 401 for POST without token")
    void shouldReturn401WhenCreatingWithoutToken() {
        webTestClient.post()
                .uri("/api/v1/maintenance-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SAMPLE_CREATE_PAYLOAD)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 401 for PUT without token")
    void shouldReturn401WhenUpdatingWithoutToken() {
        webTestClient.put()
                .uri("/api/v1/maintenance-orders/65f1a2b3c4d5e6f7a8b9c0d1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SAMPLE_CREATE_PAYLOAD)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 401 for GET with expired token signed by trusted RSA key")
    void shouldReturn401WithExpiredToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.EXPIRED_TOKEN_WITH_VALID_SIGNATURE)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    @DisplayName("Should return 401 for GET with incorrect issuer signed by trusted RSA key")
    void shouldReturn401WithWrongIssuer() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.WRONG_ISSUER_TOKEN_WITH_VALID_SIGNATURE)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    @DisplayName("Should return 401 for GET with malformed token")
    void shouldReturn401WithMalformedToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.MALFORMED_TOKEN_TWO_PARTS)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 401 for GET with unsigned token")
    void shouldReturn401WithUnsignedToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.NO_SIGNATURE_TOKEN)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
