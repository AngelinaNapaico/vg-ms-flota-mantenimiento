package pe.edu.vallegrande.flota.infrastructure.client.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.flota.infrastructure.client.external.dto.RecursoOperativoDTO;
import reactor.core.publisher.Mono;

/**
 * Cliente WebClient reactivo para comunicarse con vg-ms-recursos.
 * Gestiona el cambio de estado de los vehículos (EN_MANTENIMIENTO <-> DISPONIBLE).
 */
@Slf4j
@Component
public class RecursoOperativoClient {

    private final WebClient webClient;

    public RecursoOperativoClient(
            WebClient.Builder webClientBuilder,
            @Value("${services.recursos.url:http://localhost:9004}") String recursosUrl) {
        this.webClient = webClientBuilder
                .baseUrl(recursosUrl + "/api/recursos-operativos")
                .build();
    }

    public Mono<Void> inhabilitarRecursoPorMantenimiento(String recursoId, String token) {
        log.info("Llamando a vg-ms-recursos para pasar recurso {} a EN_MANTENIMIENTO", recursoId);

        return webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/estado")
                        .queryParam("estado", "EN_MANTENIMIENTO")
                        .build(recursoId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(RecursoOperativoDTO.class)
                .doOnSuccess(dto -> log.info("Recurso {} inhabilitado exitosamente en vg-ms-recursos", recursoId))
                .doOnError(err -> log.error("Error al inhabilitar recurso en vg-ms-recursos: {}", err.getMessage()))
                .then();
    }

    public Mono<Void> habilitarRecursoPorAltaOperativa(String recursoId, String token) {
        log.info("Llamando a vg-ms-recursos para retornar recurso {} a DISPONIBLE", recursoId);

        return webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/estado")
                        .queryParam("estado", "DISPONIBLE")
                        .build(recursoId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(RecursoOperativoDTO.class)
                .doOnSuccess(dto -> log.info("Recurso {} habilitado exitosamente a DISPONIBLE", recursoId))
                .doOnError(err -> log.error("Error al habilitar recurso en vg-ms-recursos: {}", err.getMessage()))
                .then();
    }

    public Mono<Boolean> validarRecursoExiste(String recursoId, String token) {
        log.info("Validando existencia del recurso {} en vg-ms-recursos", recursoId);

        return webClient.get()
                .uri("/{id}", recursoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(RecursoOperativoDTO.class)
                .map(dto -> dto != null && dto.getId() != null)
                .onErrorResume(e -> {
                    log.warn("El recurso {} no pudo ser verificado o no existe: {}", recursoId, e.getMessage());
                    return Mono.just(false);
                });
    }
}
