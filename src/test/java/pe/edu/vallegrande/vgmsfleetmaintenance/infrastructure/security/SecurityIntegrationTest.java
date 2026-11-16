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
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.MaintenanceOrderRepository;

/**
 * Security Integration Tests for Fleet Maintenance Microservice.
 * SEC-023-A: Tests protected endpoints without token return 401 Unauthorized.
 * SEC-023-B: Tests expired tokens or incorrect issuer return 401 Unauthorized.
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
    // SEC-023-A: Solicitudes sin token (401 Unauthorized)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("SEC-023-A: GET /api/v1/maintenance-orders sin token debe retornar 401")
    void shouldReturn401WhenListingWithoutToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("SEC-023-A: POST /api/v1/maintenance-orders sin token debe retornar 401")
    void shouldReturn401WhenCreatingWithoutToken() {
        webTestClient.post()
                .uri("/api/v1/maintenance-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SAMPLE_CREATE_PAYLOAD)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("SEC-023-A: PUT /api/v1/maintenance-orders/{id} sin token debe retornar 401")
    void shouldReturn401WhenUpdatingWithoutToken() {
        webTestClient.put()
                .uri("/api/v1/maintenance-orders/65f1a2b3c4d5e6f7a8b9c0d1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SAMPLE_CREATE_PAYLOAD)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEC-023-B: Tokens expirados, emisor incorrecto o malformados (401 Unauthorized)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("SEC-023-B: GET con token expirado debe retornar 401")
    void shouldReturn401WithExpiredToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.EXPIRED_TOKEN_WITH_VALID_SIGNATURE)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("SEC-023-B: GET con issuer incorrecto debe retornar 401")
    void shouldReturn401WithWrongIssuer() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.WRONG_ISSUER_TOKEN_WITH_VALID_SIGNATURE)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("SEC-023-B: GET con token malformado debe retornar 401")
    void shouldReturn401WithMalformedToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.MALFORMED_TOKEN_TWO_PARTS)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("SEC-023-B: GET con token sin firma debe retornar 401")
    void shouldReturn401WithUnsignedToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.NO_SIGNATURE_TOKEN)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
