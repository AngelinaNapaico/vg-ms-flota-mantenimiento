package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.AuditLogRepository;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.MaintenanceOrderRepository;

/**
 * Authentication Integration Tests.
 * Verifies authentication rejection with 401 Unauthorized for missing, expired or invalid tokens.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("SecurityIntegrationTest - Endpoint Protection & JWT Verification")
class SecurityIntegrationTest {

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

    // ═══════════════════════════════════════════════════════════════════════════
        // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should return 401 for GET without token")
    void shouldReturn401WhenListingWithoutToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .exchange()
                .expectStatus().isUnauthorized();
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

    // ═══════════════════════════════════════════════════════════════════════════
        // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should return 401 for GET with expired token")
    void shouldReturn401WithExpiredToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.EXPIRED_TOKEN_WITH_VALID_SIGNATURE)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 401 for GET with incorrect issuer")
    void shouldReturn401WithWrongIssuer() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.WRONG_ISSUER_TOKEN_WITH_VALID_SIGNATURE)
                .exchange()
                .expectStatus().isUnauthorized();
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
