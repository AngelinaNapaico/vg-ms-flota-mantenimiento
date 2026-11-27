package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.security;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.AuditLogRepository;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.repository.MaintenanceOrderRepository;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * End-to-end Authentication & Authorization Integration Tests.
 * Exercises the real JwtDecoderConfig and SecurityConfig filter chain against
 * an embedded local JWKS endpoint serving the RSA test key, verifying real
 * signature, expiration, issuer, and role-based 401/403 enforcement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("SecurityIntegrationTest - Real JwtDecoderConfig & Endpoint Protection")
class SecurityIntegrationTest {

    private static HttpServer jwksServer;
    private static String jwksUri;

    static {
        try {
            jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            jwksServer.createContext("/.well-known/jwks.json", exchange -> {
                byte[] body = TestJwtTokens.JWKS_JSON.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            jwksServer.start();
            jwksUri = "http://localhost:" + jwksServer.getAddress().getPort() + "/.well-known/jwks.json";
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start embedded JWKS server for security tests", e);
        }
    }

    @DynamicPropertySource
    static void registerJwtProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> jwksUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> TestJwtTokens.EXPECTED_ISSUER);
        registry.add("keycloak.jwk-set-uri", () -> jwksUri);
        registry.add("keycloak.issuer-uri", () -> TestJwtTokens.EXPECTED_ISSUER);
    }

    @AfterAll
    static void stopJwksServer() {
        if (jwksServer != null) {
            jwksServer.stop(0);
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
            "entityId": "65f1a2b3c4d5e6f7a8b9c001",
            "maintenanceType": "PREVENTIVE",
            "priority": "HIGH",
            "failureReason": "Preventive maintenance check",
            "reportedMileage": 45000
        }
    """;

    private static final String SAMPLE_WORKSHOP_PAYLOAD = """
        {
            "workshopName": "Central Municipal Workshop",
            "estimatedCost": 350.00
        }
    """;

    @BeforeEach
    void setupRepositoryDefaults() {
        when(repository.findByEntityId(anyString())).thenReturn(Flux.empty());
    }

    @Test
    @DisplayName("Should return 200 OK for GET with valid RSA-signed token verified by real JwtDecoderConfig")
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
    @DisplayName("Should return 401 for GET with expired RSA-signed token via real JwtDecoderConfig")
    void shouldReturn401WithExpiredToken() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.EXPIRED_TOKEN_WITH_VALID_SIGNATURE)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    @Test
    @DisplayName("Should return 401 for GET with wrong issuer RSA-signed token via real JwtDecoderConfig")
    void shouldReturn401WithWrongIssuer() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.WRONG_ISSUER_TOKEN_WITH_VALID_SIGNATURE)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    @Test
    @DisplayName("Should return 401 for GET with token signed by an untrusted RSA key")
    void shouldReturn401WithUntrustedSignature() {
        webTestClient.get()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.UNTRUSTED_SIGNATURE_TOKEN)
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

    @Test
    @DisplayName("Should return 403 Forbidden for real signed JWT with CIUDADANO role on protected write operations")
    void shouldReturn403ForValidCitizenJwtOnWriteOperations() {
        webTestClient.post()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.VALID_CITIZEN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SAMPLE_CREATE_PAYLOAD)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.error").isEqualTo("Forbidden")
                .jsonPath("$.message").isEqualTo("Access denied: insufficient permissions");

        webTestClient.put()
                .uri("/api/v1/maintenance-orders/65f1a2b3c4d5e6f7a8b9c0d1")
                .header("Authorization", "Bearer " + TestJwtTokens.VALID_CITIZEN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"remarks\":\"Unauthorized citizen update\"}")
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.delete()
                .uri("/api/v1/maintenance-orders/65f1a2b3c4d5e6f7a8b9c0d1")
                .header("Authorization", "Bearer " + TestJwtTokens.VALID_CITIZEN_TOKEN)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should return 403 Forbidden for real signed JWT with ANALISTA and MUNICIPALIDAD_ADMIN roles on write operations")
    void shouldReturn403ForValidReadOnlyRolesJwtOnWriteOperations() {
        webTestClient.post()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.VALID_ANALYST_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SAMPLE_CREATE_PAYLOAD)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.error").isEqualTo("Forbidden");

        webTestClient.patch()
                .uri("/api/v1/maintenance-orders/65f1a2b3c4d5e6f7a8b9c0d1/assign-workshop")
                .header("Authorization", "Bearer " + TestJwtTokens.VALID_ANALYST_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SAMPLE_WORKSHOP_PAYLOAD)
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.post()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.VALID_MUNICIPALITY_ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SAMPLE_CREATE_PAYLOAD)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should return 403 for authenticated request targeting undeclared route or HTTP method")
    void shouldDenyUndeclaredRouteAndMethodWithValidToken() {
        webTestClient.get()
                .uri("/api/v1/internal/undocumented")
                .header("Authorization", "Bearer " + TestJwtTokens.VALID_TOKEN)
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.head()
                .uri("/api/v1/maintenance-orders")
                .header("Authorization", "Bearer " + TestJwtTokens.VALID_TOKEN)
                .exchange()
                .expectStatus().isForbidden();
    }
}
