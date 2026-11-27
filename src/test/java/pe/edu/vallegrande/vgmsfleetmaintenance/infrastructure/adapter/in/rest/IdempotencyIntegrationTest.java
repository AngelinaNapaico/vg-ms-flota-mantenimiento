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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.MaintenanceOrderDocument;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.AuditLogRepository;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.MaintenanceOrderRepository;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Idempotency & Concurrency Integration Tests.
 * Verifies that retrying creation of an active maintenance order for the same resource
 * is rejected without creating duplicate records or altering the persisted order state.
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
    private AuditLogRepository auditLogRepository;

    @MockBean
    private ResourceClientPort resourceClientPort;

    @MockBean
    private ReactiveMongoTemplate mongoTemplate;

    private static final String TARGET_RESOURCE_ID = "65f1a2b3c4d5e6f7a8b9c999";
    private static final String TARGET_ENTITY_ID = "65f1a2b3c4d5e6f7a8b9c888";

    private List<MaintenanceOrderDocument> persistedOrders;

    @BeforeEach
    void setupInMemoryRepository() {
        persistedOrders = new ArrayList<>();

        when(resourceClientPort.validateResourceBelongsToEntity(eq(TARGET_RESOURCE_ID), eq(TARGET_ENTITY_ID)))
                .thenReturn(Mono.empty());
        when(resourceClientPort.disableResourceForMaintenance(eq(TARGET_RESOURCE_ID)))
                .thenReturn(Mono.empty());
        when(repository.existsByResourceIdAndEntityIdAndOrderStatusNotIn(
                eq(TARGET_RESOURCE_ID), eq(TARGET_ENTITY_ID), anyCollection()))
                .thenAnswer(inv -> Mono.just(!persistedOrders.isEmpty()));
        when(repository.save(any(MaintenanceOrderDocument.class)))
                .thenAnswer(inv -> {
                    MaintenanceOrderDocument doc = inv.getArgument(0);
                    doc.setId("order-persisted-" + (persistedOrders.size() + 1));
                    persistedOrders.add(doc);
                    return Mono.just(doc);
                });
        when(auditLogRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    @Test
    @DisplayName("Should create first order and reject retry without persisting duplicate records")
    void shouldPreventDuplicateActiveOrderForSameResource() {
        String firstPayload = """
            {
                "resourceId": "%s",
                "resourcePlate": "EGA-999",
                "entityId": "%s",
                "maintenanceType": "PREVENTIVE",
                "priority": "MEDIUM",
                "failureReason": "Initial order attempt"
            }
        """.formatted(TARGET_RESOURCE_ID, TARGET_ENTITY_ID);

        String duplicateRetryPayload = """
            {
                "resourceId": "%s",
                "resourcePlate": "EGA-999",
                "entityId": "%s",
                "maintenanceType": "PREVENTIVE",
                "priority": "MEDIUM",
                "failureReason": "Second order attempt"
            }
        """.formatted(TARGET_RESOURCE_ID, TARGET_ENTITY_ID);

        WebTestClient authenticatedClient = webTestClient.mutateWith(mockJwt()
                .authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_ENTIDAD"))
                .jwt(jwt -> jwt
                        .subject("operator-1")
                        .claim("preferred_username", "operator-1")
                        .claim("eid", TARGET_ENTITY_ID)
                        .claim("roles", List.of("OPERADOR_ENTIDAD"))));

        authenticatedClient.post()
                .uri("/api/v1/maintenance-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(firstPayload)
                .exchange()
                .expectStatus().isCreated();

        assertEquals(1, persistedOrders.size());
        assertEquals("REGISTERED", persistedOrders.get(0).getOrderStatus());
        assertEquals("Initial order attempt", persistedOrders.get(0).getFailureReason());

        authenticatedClient.post()
                .uri("/api/v1/maintenance-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(duplicateRetryPayload)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        assertEquals(1, persistedOrders.size());
        assertEquals("REGISTERED", persistedOrders.get(0).getOrderStatus());
        assertEquals("Initial order attempt", persistedOrders.get(0).getFailureReason());
        verify(repository, times(1)).save(any(MaintenanceOrderDocument.class));
    }
}
