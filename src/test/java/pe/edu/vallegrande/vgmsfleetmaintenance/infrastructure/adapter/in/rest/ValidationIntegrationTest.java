package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest;

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

/**
 * Validation Integration Tests.
 * Verifies input validation rejection against manipulated payloads,
 * returning HTTP 422 Unprocessable Entity.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("ValidationIntegrationTest - Payload Validation & 422 Verification")
class ValidationIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MaintenanceOrderRepository repository;

    @MockBean
    private ResourceClientPort resourceClientPort;

    @MockBean
    private ReactiveMongoTemplate mongoTemplate;

    @Test
    @WithMockUser(roles = "OPERADOR_ENTIDAD")
    @DisplayName("Should return 422 when resourceId is blank")
    void shouldReturn422WhenResourceIdIsBlank() {
        String invalidPayload = """
            {
                "resourceId": "",
                "resourcePlate": "EGA-123",
                "failureReason": "Valid reason"
            }
        """;

        webTestClient.post()
                .uri("/api/v1/maintenance-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidPayload)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Unprocessable Entity")
                .jsonPath("$.errors").exists();
    }

    @Test
    @WithMockUser(roles = "OPERADOR_ENTIDAD")
    @DisplayName("Should return 422 when mileage is negative")
    void shouldReturn422WhenMileageIsNegative() {
        String invalidPayload = """
            {
                "resourceId": "65f1a2b3c4d5e6f7a8b9c0d1",
                "resourcePlate": "EGA-123",
                "failureReason": "Valid reason",
                "reportedMileage": -50
            }
        """;

        webTestClient.post()
                .uri("/api/v1/maintenance-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidPayload)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.errors").exists();
    }
}
