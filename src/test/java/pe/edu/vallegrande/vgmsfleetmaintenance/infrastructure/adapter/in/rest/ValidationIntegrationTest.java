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
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.AuditLogRepository;
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
    private AuditLogRepository auditLogRepository;

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
                "entityId": "65f1a2b3c4d5e6f7a8b9c0e2",
                "maintenanceType": "PREVENTIVE",
                "priority": "HIGH",
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

    @Test
    @WithMockUser(roles = "OPERADOR_ENTIDAD")
    @DisplayName("Should return 422 when update incidentId contains disallowed characters")
    void shouldReturn422WhenUpdateIncidentIdIsInvalid() {
        String invalidUpdatePayload = """
            {
                "incidentId": "invalid$script<>"
            }
        """;

        webTestClient.put()
                .uri("/api/v1/maintenance-orders/order-001")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidUpdatePayload)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.errors").exists();
    }

    @Test
    @WithMockUser(roles = "OPERADOR_ENTIDAD")
    @DisplayName("Should return 422 when PUT update payload has all fields null or empty")
    void shouldReturn422WhenUpdatePayloadIsEmpty() {
        webTestClient.put()
                .uri("/api/v1/maintenance-orders/order-001")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Unprocessable Entity")
                .jsonPath("$.errors").exists();
    }

    @Test
    @WithMockUser(roles = "OPERADOR_ENTIDAD")
    @DisplayName("Should return 422 when estimatedDeliveryDate is in the past")
    void shouldReturn422WhenEstimatedDeliveryDateIsInThePast() {
        String pastWorkshopPayload = """
            {
                "workshopName": "Central Workshop",
                "estimatedDeliveryDate": "2020-01-01T10:00:00"
            }
        """;

        webTestClient.patch()
                .uri("/api/v1/maintenance-orders/order-001/assign-workshop")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(pastWorkshopPayload)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.errors").exists();
    }

    @Test
    @WithMockUser(roles = "OPERADOR_ENTIDAD")
    @DisplayName("Should return 422 when estimatedCost exceeds scale of 2 decimals or business maximum")
    void shouldReturn422WhenEstimatedCostViolatesPrecisionOrMaximum() {
        String excessiveScalePayload = """
            {
                "workshopName": "Central Workshop",
                "estimatedCost": 1500.125
            }
        """;

        webTestClient.patch()
                .uri("/api/v1/maintenance-orders/order-001/assign-workshop")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(excessiveScalePayload)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.errors").exists();

        String excessiveMaxPayload = """
            {
                "workshopName": "Central Workshop",
                "estimatedCost": 100000000.00
            }
        """;

        webTestClient.patch()
                .uri("/api/v1/maintenance-orders/order-001/assign-workshop")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(excessiveMaxPayload)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.errors").exists();
    }

    @Test
    @WithMockUser(roles = "OPERADOR_ENTIDAD")
    @DisplayName("Should return 422 when finalCost exceeds scale of 2 decimals or business maximum")
    void shouldReturn422WhenFinalCostViolatesPrecisionOrMaximum() {
        String invalidDischargePayload = """
            {
                "technicalVerdict": "Approved after road test",
                "exitMileage": 50000,
                "finalCost": 950.999
            }
        """;

        webTestClient.post()
                .uri("/api/v1/maintenance-orders/order-001/technical-discharge")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidDischargePayload)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.errors").exists();
    }
}
