package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.exception.custom.UpstreamServiceException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceClientAdapterTest {

    @Test
    @DisplayName("Should call PATCH /api/v1/resources/{id}/availability?availability=UNDER_MAINTENANCE with Bearer token")
    void shouldCallDisableResourceForMaintenanceWithEnglishPath() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        AtomicReference<HttpMethod> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedAuth = new AtomicReference<>();

        ExchangeFunction exchangeFunction = request -> {
            capturedUri.set(request.url());
            capturedMethod.set(request.method());
            capturedAuth.set(request.headers().getFirst(HttpHeaders.AUTHORIZATION));
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        ResourceClientAdapter adapter = new ResourceClientAdapter(builder, "http://localhost:9004");

        StepVerifier.create(adapter.disableResourceForMaintenance("rec-123", "jwt-token-abc"))
                .verifyComplete();

        assertEquals(HttpMethod.PATCH, capturedMethod.get());
        assertEquals("Bearer jwt-token-abc", capturedAuth.get());
        assertEquals("http://localhost:9004/api/v1/resources/rec-123/availability?availability=UNDER_MAINTENANCE", capturedUri.get().toString());
    }

    @Test
    @DisplayName("Should call PATCH /api/v1/resources/{id}/availability?availability=AVAILABLE with Bearer token")
    void shouldCallEnableResourceToAvailableWithEnglishPath() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        AtomicReference<HttpMethod> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedAuth = new AtomicReference<>();

        ExchangeFunction exchangeFunction = request -> {
            capturedUri.set(request.url());
            capturedMethod.set(request.method());
            capturedAuth.set(request.headers().getFirst(HttpHeaders.AUTHORIZATION));
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        ResourceClientAdapter adapter = new ResourceClientAdapter(builder, "http://localhost:9004");

        StepVerifier.create(adapter.enableResourceToAvailable("rec-123", "jwt-token-xyz"))
                .verifyComplete();

        assertEquals(HttpMethod.PATCH, capturedMethod.get());
        assertEquals("Bearer jwt-token-xyz", capturedAuth.get());
        assertEquals("http://localhost:9004/api/v1/resources/rec-123/availability?availability=AVAILABLE", capturedUri.get().toString());
    }

    @Test
    @DisplayName("Should unwrap ApiResponse data payload and validate resource entity ownership")
    void shouldUnwrapApiResponseAndValidateOwnership() {
        String wrappedJson = """
                {
                  "success": true,
                  "data": {
                    "id": "65f1a2b3c4d5e6f7a8b9c001",
                    "entityId": "ent-001",
                    "availability": "AVAILABLE"
                  },
                  "message": "Resource found"
                }
                """;
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(wrappedJson)
                .build());
        ResourceClientAdapter adapter = new ResourceClientAdapter(
                WebClient.builder().exchangeFunction(exchangeFunction), "http://localhost:9004");

        StepVerifier.create(adapter.validateResourceBelongsToEntity(
                        "65f1a2b3c4d5e6f7a8b9c001", "ent-001", "jwt-token"))
                .verifyComplete();

        StepVerifier.create(adapter.validateResourceBelongsToEntity(
                        "65f1a2b3c4d5e6f7a8b9c001", "ent-999", "jwt-token"))
                .expectError(DomainException.class)
                .verify();
    }

    @Test
    @DisplayName("Should translate 4xx client error to DomainException without propagating WebClient cause")
    void shouldTranslate4xxToDomainExceptionWithoutTechnicalCause() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        ResourceClientAdapter adapter = new ResourceClientAdapter(
                WebClient.builder().exchangeFunction(exchangeFunction), "http://localhost:9004");

        StepVerifier.create(adapter.disableResourceForMaintenance("rec-404", "jwt-token"))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(DomainException.class, err);
                    assertNull(err.getCause());
                })
                .verify();
    }

    @Test
    @DisplayName("Should translate 5xx server error to UpstreamServiceException with sanitized technical metadata")
    void shouldTranslate5xxToUpstreamExceptionWithoutTechnicalCause() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build());
        ResourceClientAdapter adapter = new ResourceClientAdapter(
                WebClient.builder().exchangeFunction(exchangeFunction), "http://localhost:9004");

        StepVerifier.create(adapter.enableResourceToAvailable("rec-503", "jwt-token"))
                .expectErrorSatisfies(err -> {
                    UpstreamServiceException ex = assertInstanceOf(UpstreamServiceException.class, err);
                    assertNull(ex.getCause());
                    assertEquals("enable", ex.getOperation());
                    assertEquals(503, ex.getUpstreamStatus());
                    assertTrue(ex.getTechnicalCause().startsWith("HTTP_503:"));
                })
                .verify();
    }

    @Test
    @DisplayName("Should translate transport failure to UpstreamServiceException with sanitized technical metadata")
    void shouldTranslateTransportFailureWithoutTechnicalCause() {
        ExchangeFunction exchangeFunction = request -> Mono.error(new RuntimeException("Connection refused"));
        ResourceClientAdapter adapter = new ResourceClientAdapter(
                WebClient.builder().exchangeFunction(exchangeFunction), "http://localhost:9004");

        StepVerifier.create(adapter.validateResourceBelongsToEntity("rec-net", "ent-001", "jwt-token"))
                .expectErrorSatisfies(err -> {
                    UpstreamServiceException ex = assertInstanceOf(UpstreamServiceException.class, err);
                    assertNull(ex.getCause());
                    assertEquals("validate", ex.getOperation());
                    assertNull(ex.getUpstreamStatus());
                    assertEquals("TRANSPORT:RuntimeException", ex.getTechnicalCause());
                })
                .verify();
    }

    @Test
    @DisplayName("Should reject inter-service calls when bearer token is missing without issuing HTTP requests")
    void shouldRejectInterServiceCallsWithoutBearerToken() {
        AtomicInteger callCount = new AtomicInteger(0);
        ExchangeFunction exchangeFunction = request -> {
            callCount.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };
        ResourceClientAdapter adapter = new ResourceClientAdapter(
                WebClient.builder().exchangeFunction(exchangeFunction), "http://localhost:9004");

        StepVerifier.create(adapter.validateResourceBelongsToEntity("rec-001", "ent-001", ""))
                .expectError(AuthenticationCredentialsNotFoundException.class)
                .verify();
        StepVerifier.create(adapter.disableResourceForMaintenance("rec-001", null))
                .expectError(AuthenticationCredentialsNotFoundException.class)
                .verify();
        StepVerifier.create(adapter.enableResourceToAvailable("rec-001", "   "))
                .expectError(AuthenticationCredentialsNotFoundException.class)
                .verify();

        assertEquals(0, callCount.get());
    }
}
