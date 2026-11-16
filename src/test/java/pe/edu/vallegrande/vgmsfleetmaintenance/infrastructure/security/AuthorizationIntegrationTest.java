package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.MaintenanceOrderRepository;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Authorization Integration Tests (RBAC).
 * SEC-023-C: Verifies that authenticated users with insufficient permissions receive 403 Forbidden.
 * SEC-007: Verifies least-privilege enforcement (MUNICIPALIDAD_ADMIN is strictly read-only).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("AuthorizationIntegrationTest - Role-Based Access Control")
class AuthorizationIntegrationTest {

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
            "failureReason": "Routine check",
            "reportedMileage": 50000
        }
    """;

    @Test
    @WithMockUser(roles = "CIUDADANO")
    @DisplayName("SEC-023-C: CIUDADANO POST /api/v1/maintenance-orders debe retornar 403 Forbidden")
    void shouldRejectPostForCiudadano() {
        webTestClient.post()
                .uri("/api/v1/maintenance-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SAMPLE_CREATE_PAYLOAD)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(roles = "ANALISTA")
    @DisplayName("SEC-023-C: ANALISTA POST /api/v1/maintenance-orders debe retornar 403 Forbidden")
    void shouldRejectPostForAnalista() {
        webTestClient.post()
                .uri("/api/v1/maintenance-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SAMPLE_CREATE_PAYLOAD)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(roles = "MUNICIPALIDAD_ADMIN")
    @DisplayName("SEC-007: MUNICIPALIDAD_ADMIN POST /api/v1/maintenance-orders debe retornar 403 Forbidden (solo lectura)")
    void shouldRejectPostForMunicipalidadAdmin() {
        webTestClient.post()
                .uri("/api/v1/maintenance-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SAMPLE_CREATE_PAYLOAD)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(roles = "ANALISTA")
    @DisplayName("SEC-023-C: ANALISTA GET /api/v1/maintenance-orders debe permitirse (no 403)")
    void shouldAllowGetForAnalista() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .exchange()
                .expectStatus().value(status -> assertNotEquals(HttpStatus.FORBIDDEN.value(), status));
    }
}
