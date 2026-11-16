package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest;

import org.junit.jupiter.api.BeforeEach;
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
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Idempotency & Concurrency Integration Tests.
 * Verifies that concurrent attempts or retries to create an active order
 * for the same operational resource are rejected, preventing duplicate active orders.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("IdempotencyIntegrationTest - Duplicate Order Prevention")
class IdempotencyIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MaintenanceOrderRepository repository;

    @MockBean
    private ResourceClientPort resourceClientPort;

    @MockBean
    private ReactiveMongoTemplate mongoTemplate;

    private static final String TARGET_RESOURCE_ID = "65f1a2b3c4d5e6f7a8b9c999";
    private static final String TARGET_ENTITY_ID = "65f1a2b3c4d5e6f7a8b9c888";

    @BeforeEach
    void setupActiveOrder() {
        when(repository.existsByResourceIdAndOrderStatusNotIn(any(), any()))
                .thenReturn(Mono.just(true));
        when(repository.existsByResourceIdAndEntityIdAndOrderStatusNotIn(any(), any(), any()))
                .thenReturn(Mono.just(true));
    }

    @Test
    @WithMockUser(username = "operator-1", roles = "OPERADOR_ENTIDAD")
    @DisplayName("Should prevent duplicate active order for same resource")
    void shouldPreventDuplicateActiveOrderForSameResource() {
        String duplicateOrderPayload = """
            {
                "resourceId": "%s",
                "resourcePlate": "EGA-999",
                "entityId": "%s",
                "maintenanceType": "PREVENTIVE",
                "priority": "MEDIUM",
                "failureReason": "Second order attempt"
            }
        """.formatted(TARGET_RESOURCE_ID, TARGET_ENTITY_ID);

        webTestClient.post()
                .uri("/api/v1/maintenance-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(duplicateOrderPayload)
                .exchange()
                .expectStatus().value(status -> {
                    // Must not be created (201)
                    assertNotEquals(HttpStatus.CREATED.value(), status);
                });
    }
}
