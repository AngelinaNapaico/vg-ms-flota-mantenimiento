package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import reactor.core.publisher.Mono;

/**
 * Secondary Outbound WebClient Adapter for vg-ms-recursos microservice.
 * Implements ResourceClientPort with Bearer token propagation and technical
 * error translation (STD-072, STD-074, STD-077).
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

    @Override
    public Mono<Void> disableResourceForMaintenance(String resourceId, String bearerToken) {
        log.info("Calling resource service to set availability to EN_MANTENIMIENTO for resource {}", resourceId);

        return webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/availability")
                        .queryParam("availability", "EN_MANTENIMIENTO")
                        .build(resourceId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.createException()
                        .flatMap(err -> {
                            log.warn("Resource client rejected operation: HTTP status {}", response.statusCode().value());
                            return Mono.error(new DomainException("External resource service rejected the operation", err));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response -> response.createException()
                        .flatMap(err -> {
                            log.error("Resource service unavailable: HTTP status {}", response.statusCode().value());
                            return Mono.error(new DomainException("External resource service is temporarily unavailable", err));
                        }))
                .toBodilessEntity()
                .doOnSuccess(entity -> log.info("Resource {} marked EN_MANTENIMIENTO in resource service", resourceId))
                .doOnError(err -> log.error("Failed setting resource to EN_MANTENIMIENTO: {}", err.getMessage(), err))
                .then();
    }

    @Override
    public Mono<Void> enableResourceToAvailable(String resourceId, String bearerToken) {
        log.info("Calling resource service to restore availability to DISPONIBLE for resource {}", resourceId);

        return webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/availability")
                        .queryParam("availability", "DISPONIBLE")
                        .build(resourceId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.createException()
                        .flatMap(err -> {
                            log.warn("Resource client rejected operation: HTTP status {}", response.statusCode().value());
                            return Mono.error(new DomainException("External resource service rejected the operation", err));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response -> response.createException()
                        .flatMap(err -> {
                            log.error("Resource service unavailable: HTTP status {}", response.statusCode().value());
                            return Mono.error(new DomainException("External resource service is temporarily unavailable", err));
                        }))
                .toBodilessEntity()
                .doOnSuccess(entity -> log.info("Resource {} restored to DISPONIBLE in resource service", resourceId))
                .doOnError(err -> log.error("Failed restoring resource to DISPONIBLE: {}", err.getMessage(), err))
                .then();
    }
}
