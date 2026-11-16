package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.dto.OperationalResourceDTO;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.mapper.ResourceClientMapper;
import reactor.core.publisher.Mono;

/**
 * Outbound WebClient adapter communicating with the external resources microservice.
 * Propagates authorization tokens and handles service-to-service communication.
 */
@Slf4j
@Component
public class ResourceClientAdapter implements ResourceClientPort {

    private final WebClient webClient;

    public ResourceClientAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${services.resource.url:${services.recursos.url:http://localhost:9004}}") String resourceServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(resourceServiceUrl + "/api/v1/resources")
                .build();
    }

    private Mono<String> resolveCurrentToken() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(jwt -> jwt.getToken().getTokenValue())
                .defaultIfEmpty("");
    }

    @Override
    public Mono<Void> disableResourceForMaintenance(String resourceId) {
        return resolveCurrentToken()
                .flatMap(token -> disableResourceForMaintenance(resourceId, token));
    }

    @Override
    public Mono<Void> enableResourceToAvailable(String resourceId) {
        return resolveCurrentToken()
                .flatMap(token -> enableResourceToAvailable(resourceId, token));
    }

    @Override
    public Mono<Void> disableResourceForMaintenance(String resourceId, String bearerToken) {
        OperationalResourceDTO payload = ResourceClientMapper.toDTO(resourceId, "EN_MANTENIMIENTO");
        log.info("Requesting status change to {} for resource {}", payload.getAvailability(), resourceId);

        var requestSpec = webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/availability")
                        .queryParam("availability", payload.getAvailability())
                        .build(resourceId));

        if (bearerToken != null && !bearerToken.isBlank()) {
            requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }

        return requestSpec
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.createException()
                        .flatMap(err -> {
                            log.warn("Resource service rejected operation with status {}, cause: {}",
                                    response.statusCode().value(), err.getMessage());
                            return Mono.error(new DomainException(
                                    "External resource service rejected the operation",
                                    err));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response -> response.createException()
                        .flatMap(err -> {
                            log.error("Resource service unavailable with status {}, cause: {}",
                                    response.statusCode().value(), err.getMessage());
                            return Mono.error(new DomainException(
                                    "External resource service is temporarily unavailable",
                                    err));
                        }))
                .toBodilessEntity()
                .doOnSuccess(entity -> log.info("Resource {} marked {} successfully", resourceId,
                        payload.getAvailability()))
                .doOnError(err -> log.warn("Failed to notify resource service for resourceId: {} - cause: {}",
                        resourceId, err.getMessage()))
                .then();
    }

    @Override
    public Mono<Void> enableResourceToAvailable(String resourceId, String bearerToken) {
        OperationalResourceDTO payload = ResourceClientMapper.toDTO(resourceId, "DISPONIBLE");
        log.info("Requesting status change to {} for resource {}", payload.getAvailability(), resourceId);

        var requestSpec = webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/availability")
                        .queryParam("availability", payload.getAvailability())
                        .build(resourceId));

        if (bearerToken != null && !bearerToken.isBlank()) {
            requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }

        return requestSpec
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.createException()
                        .flatMap(err -> {
                            log.warn("Resource service rejected operation with status {}, cause: {}",
                                    response.statusCode().value(), err.getMessage());
                            return Mono.error(new DomainException(
                                    "External resource service rejected the operation",
                                    err));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response -> response.createException()
                        .flatMap(err -> {
                            log.error("Resource service unavailable with status {}, cause: {}",
                                    response.statusCode().value(), err.getMessage());
                            return Mono.error(new DomainException(
                                    "External resource service is temporarily unavailable",
                                    err));
                        }))
                .toBodilessEntity()
                .doOnSuccess(entity -> log.info("Resource {} restored to {} successfully", resourceId,
                        payload.getAvailability()))
                .doOnError(err -> log.warn("Failed to restore availability in resource service for resourceId: {} - cause: {}",
                        resourceId, err.getMessage()))
                .then();
    }
}
