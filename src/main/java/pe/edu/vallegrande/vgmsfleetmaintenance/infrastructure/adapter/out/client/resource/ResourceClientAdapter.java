package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.dto.OperationalResourceDTO;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource.mapper.ResourceClientMapper;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.exception.custom.UpstreamServiceException;
import reactor.core.publisher.Mono;

/**
 * Outbound WebClient adapter communicating with the external resources microservice.
 * Enforces mandatory Bearer token propagation, validates ownership integrity,
 * logs sanitized diagnostic metadata, and translates upstream failures cleanly.
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

    private Mono<String> requireBearerToken(String explicitToken) {
        if (explicitToken != null && !explicitToken.isBlank()) {
            return Mono.just(explicitToken.trim());
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(jwt -> jwt.getToken().getTokenValue())
                .filter(token -> token != null && !token.isBlank())
                .switchIfEmpty(Mono.error(new AuthenticationCredentialsNotFoundException(
                        "Authenticated bearer token is required for inter-service communication")));
    }

    @Override
    public Mono<Void> disableResourceForMaintenance(String resourceId) {
        return disableResourceForMaintenance(resourceId, null);
    }

    @Override
    public Mono<Void> enableResourceToAvailable(String resourceId) {
        return enableResourceToAvailable(resourceId, null);
    }

    @Override
    public Mono<Void> validateResourceBelongsToEntity(String resourceId, String entityId) {
        return validateResourceBelongsToEntity(resourceId, entityId, null);
    }

    @Override
    public Mono<Void> validateResourceBelongsToEntity(String resourceId, String entityId, String bearerToken) {
        if (resourceId == null || resourceId.isBlank()) {
            return Mono.error(new DomainException("Resource ID is required for validation"));
        }
        if (entityId == null || entityId.isBlank()) {
            return Mono.error(new DomainException("Entity ID is required for resource validation"));
        }

        return requireBearerToken(bearerToken).flatMap(resolvedToken -> webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/{id}").build(resourceId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + resolvedToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.createException()
                        .flatMap(err -> {
                            int status = response.statusCode().value();
                            log.warn("Resource service rejected [op=validate, resourceId={}, status={}, type={}]",
                                    resourceId, status, err.getClass().getSimpleName());
                            return Mono.error(new DomainException(
                                    "Operational resource not found or access denied in resource service"));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response -> response.createException()
                        .flatMap(err -> {
                            int status = response.statusCode().value();
                            String technicalCause = buildTechnicalCause(status, err);
                            log.error("Resource service 5xx failure [op=validate, resourceId={}, status={}, cause={}]",
                                    resourceId, status, technicalCause);
                            return Mono.error(new UpstreamServiceException(
                                    "External resource service is temporarily unavailable",
                                    "validate",
                                    status,
                                    technicalCause));
                        }))
                .bodyToMono(OperationalResourceDTO.class)
                .switchIfEmpty(Mono.error(new DomainException("Operational resource record not found")))
                .flatMap(resource -> {
                    if (resource.getEntityId() == null || !resource.getEntityId().equals(entityId)) {
                        log.warn("Resource context mismatch: resource {} belongs to {} but requested by {}",
                                resourceId, resource.getEntityId(), entityId);
                        return Mono.error(new DomainException(
                                "Operational resource does not belong to the caller entity"));
                    }
                    return Mono.empty();
                })
                .onErrorMap(e -> !(e instanceof DomainException || e instanceof UpstreamServiceException),
                        err -> {
                            String technicalCause = buildTechnicalCause(null, err);
                            log.error("Resource service transport failure [op=validate, resourceId={}, cause={}]",
                                    resourceId, technicalCause);
                            return new UpstreamServiceException(
                                    "Failed to verify resource with external service",
                                    "validate",
                                    null,
                                    technicalCause);
                        })
                .then());
    }

    @Override
    public Mono<Void> disableResourceForMaintenance(String resourceId, String bearerToken) {
        OperationalResourceDTO payload = ResourceClientMapper.toDTO(resourceId, "EN_MANTENIMIENTO");
        log.info("Requesting status change to {} for resource {}", payload.getAvailability(), resourceId);

        return requireBearerToken(bearerToken).flatMap(resolvedToken -> webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/availability")
                        .queryParam("availability", payload.getAvailability())
                        .build(resourceId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + resolvedToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.createException()
                        .flatMap(err -> {
                            int status = response.statusCode().value();
                            log.warn("Resource service rejected [op=disable, resourceId={}, status={}, type={}]",
                                    resourceId, status, err.getClass().getSimpleName());
                            return Mono.error(new DomainException(
                                    "External resource service rejected the operation"));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response -> response.createException()
                        .flatMap(err -> {
                            int status = response.statusCode().value();
                            String technicalCause = buildTechnicalCause(status, err);
                            log.error("Resource service 5xx failure [op=disable, resourceId={}, status={}, cause={}]",
                                    resourceId, status, technicalCause);
                            return Mono.error(new UpstreamServiceException(
                                    "External resource service is temporarily unavailable",
                                    "disable",
                                    status,
                                    technicalCause));
                        }))
                .toBodilessEntity()
                .onErrorMap(e -> !(e instanceof DomainException || e instanceof UpstreamServiceException),
                        err -> {
                            String technicalCause = buildTechnicalCause(null, err);
                            log.error("Resource service transport failure [op=disable, resourceId={}, cause={}]",
                                    resourceId, technicalCause);
                            return new UpstreamServiceException(
                                    "Failed to communicate with external resource service",
                                    "disable",
                                    null,
                                    technicalCause);
                        })
                .doOnSuccess(entity -> log.info("Resource {} marked {} successfully", resourceId,
                        payload.getAvailability()))
                .doOnError(err -> log.warn(
                        "Failed to notify resource service for resourceId: {} - type: {}",
                        resourceId, err.getClass().getSimpleName()))
                .then());
    }

    @Override
    public Mono<Void> enableResourceToAvailable(String resourceId, String bearerToken) {
        OperationalResourceDTO payload = ResourceClientMapper.toDTO(resourceId, "DISPONIBLE");
        log.info("Requesting status change to {} for resource {}", payload.getAvailability(), resourceId);

        return requireBearerToken(bearerToken).flatMap(resolvedToken -> webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/availability")
                        .queryParam("availability", payload.getAvailability())
                        .build(resourceId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + resolvedToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.createException()
                        .flatMap(err -> {
                            int status = response.statusCode().value();
                            log.warn("Resource service rejected [op=enable, resourceId={}, status={}, type={}]",
                                    resourceId, status, err.getClass().getSimpleName());
                            return Mono.error(new DomainException(
                                    "External resource service rejected the operation"));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response -> response.createException()
                        .flatMap(err -> {
                            int status = response.statusCode().value();
                            String technicalCause = buildTechnicalCause(status, err);
                            log.error("Resource service 5xx failure [op=enable, resourceId={}, status={}, cause={}]",
                                    resourceId, status, technicalCause);
                            return Mono.error(new UpstreamServiceException(
                                    "External resource service is temporarily unavailable",
                                    "enable",
                                    status,
                                    technicalCause));
                        }))
                .toBodilessEntity()
                .onErrorMap(e -> !(e instanceof DomainException || e instanceof UpstreamServiceException),
                        err -> {
                            String technicalCause = buildTechnicalCause(null, err);
                            log.error("Resource service transport failure [op=enable, resourceId={}, cause={}]",
                                    resourceId, technicalCause);
                            return new UpstreamServiceException(
                                    "Failed to communicate with external resource service",
                                    "enable",
                                    null,
                                    technicalCause);
                        })
                .doOnSuccess(entity -> log.info("Resource {} restored to {} successfully", resourceId,
                        payload.getAvailability()))
                .doOnError(err -> log.warn(
                        "Failed to restore availability in resource service for resourceId: {} - type: {}",
                        resourceId, err.getClass().getSimpleName()))
                .then());
    }

    private String buildTechnicalCause(Integer status, Throwable err) {
        String rootType = resolveRootCauseType(err);
        if (status != null) {
            return "HTTP_" + status + ":" + rootType;
        }
        return "TRANSPORT:" + rootType;
    }

    private String resolveRootCauseType(Throwable err) {
        if (err == null) {
            return "UNKNOWN";
        }
        Throwable cursor = err;
        int depth = 0;
        while (cursor.getCause() != null && cursor.getCause() != cursor && depth < 5) {
            cursor = cursor.getCause();
            depth++;
        }
        return cursor.getClass().getSimpleName();
    }
}
