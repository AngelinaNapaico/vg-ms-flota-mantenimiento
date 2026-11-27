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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.MaintenanceOrderDocument;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.AuditLogRepository;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.MaintenanceOrderRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

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
    private AuditLogRepository auditLogRepository;

    @MockBean
    private ResourceClientPort resourceClientPort;

    @MockBean
    private ReactiveMongoTemplate mongoTemplate;

    private static final String ENTITY_A = "65f1a2b3c4d5e6f7a8b9c001";
    private static final String ENTITY_B = "65f1a2b3c4d5e6f7a8b9c002";
    private static final String ORDER_A_ID = "order-ent-a-001";
    private static final String ORDER_B_ID = "order-ent-b-002";

    private MaintenanceOrderDocument docA;
    private MaintenanceOrderDocument docB;

    @BeforeEach
    void setupFixtures() {
        docA = MaintenanceOrderDocument.builder()
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

        docB = MaintenanceOrderDocument.builder()
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
        when(repository.findByIdAndEntityId(eq(ORDER_A_ID), eq(ENTITY_A))).thenReturn(Mono.just(docA));
        when(repository.findByIdAndEntityId(eq(ORDER_B_ID), eq(ENTITY_A))).thenReturn(Mono.empty());
        when(repository.findByEntityId(eq(ENTITY_A))).thenReturn(Flux.just(docA));
        when(repository.findByEntityId(eq(ENTITY_B))).thenReturn(Flux.just(docB));
        when(repository.findAll()).thenReturn(Flux.just(docA, docB));
        when(auditLogRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    @Test
    @DisplayName("Should allow operator of entity A to read order belonging to entity A")
    void shouldAllowOperatorToReadOwnEntityOrder() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_ENTIDAD"))
                        .jwt(jwt -> jwt
                                .subject("operator-a")
                                .claim("preferred_username", "operator-a")
                                .claim("eid", ENTITY_A)
                                .claim("roles", List.of("OPERADOR_ENTIDAD"))))
                .get()
                .uri("/api/v1/maintenance-orders/" + ORDER_A_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderCode").isEqualTo("ORD-2026-AAAAAA")
                .jsonPath("$.entityId").isEqualTo(ENTITY_A);
    }

    @Test
    @DisplayName("Should reject order access across different tenants")
    void shouldNotExposeEntityBOrderToEntityA() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_ENTIDAD"))
                        .jwt(jwt -> jwt
                                .subject("operator-a")
                                .claim("preferred_username", "operator-a")
                                .claim("eid", ENTITY_A)
                                .claim("roles", List.of("OPERADOR_ENTIDAD"))))
                .get()
                .uri("/api/v1/maintenance-orders/" + ORDER_B_ID)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.orderCode").doesNotExist()
                .jsonPath("$.resourceId").doesNotExist();
    }

    @Test
    @DisplayName("Should reject order mutation across different tenants")
    void shouldRejectMutationAcrossEntities() {
        String originalReason = docB.getFailureReason();
        String originalEntity = docB.getEntityId();
        String originalResource = docB.getResourceId();

        String updatePayload = """
                    {
                        "failureReason": "Unauthorized modification attempt",
                        "reportedMileage": 60000
                    }
                """;

        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_ENTIDAD"))
                        .jwt(jwt -> jwt
                                .subject("operator-a")
                                .claim("preferred_username", "operator-a")
                                .claim("eid", ENTITY_A)
                                .claim("roles", List.of("OPERADOR_ENTIDAD"))))
                .put()
                .uri("/api/v1/maintenance-orders/" + ORDER_B_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatePayload)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.failureReason").doesNotExist();

        assertEquals(originalReason, docB.getFailureReason());
        assertEquals(originalEntity, docB.getEntityId());
        assertEquals(originalResource, docB.getResourceId());
    }

    @Test
    @DisplayName("Should verify order remains unchanged when unauthorized tenant attempts mutation")
    void shouldVerifyOrderRemainsUnchangedOnCrossTenantMutation() {
        String originalReason = docB.getFailureReason();

        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_ENTIDAD"))
                        .jwt(jwt -> jwt
                                .subject("operator-a")
                                .claim("preferred_username", "operator-a")
                                .claim("eid", ENTITY_A)
                                .claim("roles", List.of("OPERADOR_ENTIDAD"))))
                .patch()
                .uri("/api/v1/maintenance-orders/" + ORDER_B_ID + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"reason\": \"Illegal cancellation attempt\"}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.orderCode").doesNotExist();

        assertEquals(originalReason, docB.getFailureReason());
        assertEquals(ENTITY_B, docB.getEntityId());
    }

    @Test
    @DisplayName("Should allow super admin global access across tenants")
    void shouldAllowSuperAdminAccess() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                        .jwt(jwt -> jwt
                                .subject("super-admin")
                                .claim("preferred_username", "super-admin")
                                .claim("roles", List.of("SUPER_ADMIN"))))
                .get()
                .uri("/api/v1/maintenance-orders/" + ORDER_A_ID)
                .exchange()
                .expectStatus().isOk();

        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                        .jwt(jwt -> jwt
                                .subject("super-admin")
                                .claim("preferred_username", "super-admin")
                                .claim("roles", List.of("SUPER_ADMIN"))))
                .get()
                .uri("/api/v1/maintenance-orders/" + ORDER_B_ID)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Should return only own tenant orders on list and empty list on foreign entity query")
    void shouldIsolateListAndEntityQueriesByTenant() {
        WebTestClient operatorAClient = webTestClient.mutateWith(mockJwt()
                .authorities(new SimpleGrantedAuthority("ROLE_OPERADOR_ENTIDAD"))
                .jwt(jwt -> jwt
                        .subject("operator-a")
                        .claim("preferred_username", "operator-a")
                        .claim("eid", ENTITY_A)
                        .claim("roles", List.of("OPERADOR_ENTIDAD"))));

        operatorAClient.get()
                .uri("/api/v1/maintenance-orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].orderCode").isEqualTo("ORD-2026-AAAAAA")
                .jsonPath("$[0].entityId").isEqualTo(ENTITY_A);

        operatorAClient.get()
                .uri("/api/v1/maintenance-orders/entity/" + ENTITY_B)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("Should allow super admin to list orders across all tenants")
    void shouldAllowSuperAdminToListAllTenants() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                        .jwt(jwt -> jwt
                                .subject("super-admin")
                                .claim("preferred_username", "super-admin")
                                .claim("roles", List.of("SUPER_ADMIN"))))
                .get()
                .uri("/api/v1/maintenance-orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }
}
