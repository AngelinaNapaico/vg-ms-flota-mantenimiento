package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

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
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.MaintenanceOrderDocument;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.MaintenanceOrderRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Multi-Tenancy Integration Tests.
 * Verifies tenant data isolation between distinct entities and ownership constraints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("MultiTenancyIntegrationTest - Cross-Tenant Isolation")
class MultiTenancyIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MaintenanceOrderRepository repository;

    @MockBean
    private ResourceClientPort resourceClientPort;

    @MockBean
    private ReactiveMongoTemplate mongoTemplate;

    private static final String ENTITY_A = "65f1a2b3c4d5e6f7a8b9c001";
    private static final String ENTITY_B = "65f1a2b3c4d5e6f7a8b9c002";
    private static final String ORDER_A_ID = "order-ent-a-001";
    private static final String ORDER_B_ID = "order-ent-b-002";

    @BeforeEach
    void setupFixtures() {
        MaintenanceOrderDocument docA = MaintenanceOrderDocument.builder()
                .id(ORDER_A_ID)
                .orderCode("ORD-2026-AAAAAA")
                .resourceId("REC-001")
                .resourcePlate("EGA-101")
                .entityId(ENTITY_A)
                .orderStatus("REGISTERED")
                .failureReason("Brake inspection")
                .status("A")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        MaintenanceOrderDocument docB = MaintenanceOrderDocument.builder()
                .id(ORDER_B_ID)
                .orderCode("ORD-2026-BBBBBB")
                .resourceId("REC-002")
                .resourcePlate("EGA-202")
                .entityId(ENTITY_B)
                .orderStatus("REGISTERED")
                .failureReason("Oil change")
                .status("A")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(repository.findById(eq(ORDER_A_ID))).thenReturn(Mono.just(docA));
        when(repository.findById(eq(ORDER_B_ID))).thenReturn(Mono.just(docB));
    }

    @Test
    @WithMockUser(username = "operator-a", roles = "OPERADOR_ENTIDAD")
    @DisplayName("Should reject order access across different tenants")
    void shouldNotExposeEntityBOrderToEntityA() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders/" + ORDER_B_ID)
                .exchange()
                .expectStatus().value(status -> assertNotEquals(HttpStatus.OK.value(), status));
    }

    @Test
    @WithMockUser(username = "operator-a", roles = "OPERADOR_ENTIDAD")
    @DisplayName("Should reject order mutation across different tenants")
    void shouldRejectMutationAcrossEntities() {
        String updatePayload = """
            {
                "failureReason": "Unauthorized modification attempt",
                "reportedMileage": 60000
            }
        """;

        webTestClient.put()
                .uri("/api/v1/maintenance-orders/" + ORDER_B_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatePayload)
                .exchange()
                .expectStatus().value(status -> assertNotEquals(HttpStatus.OK.value(), status));
    }

    @Test
    @WithMockUser(username = "super-admin", roles = "SUPER_ADMIN")
    @DisplayName("Should allow super admin global access across tenants")
    void shouldAllowSuperAdminAccess() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders/" + ORDER_A_ID)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/v1/maintenance-orders/" + ORDER_B_ID)
                .exchange()
                .expectStatus().isOk();
    }
}
