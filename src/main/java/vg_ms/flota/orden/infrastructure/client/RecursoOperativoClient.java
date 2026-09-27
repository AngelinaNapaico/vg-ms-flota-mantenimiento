package vg_ms.flota.orden.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import vg_ms.flota.orden.application.port.RecursoOperativoPort;
import vg_ms.flota.orden.infrastructure.client.dto.RecursoOperativoResponseDTO;
import vg_ms.flota.shared.exception.DomainException;

@Slf4j
@Component
public class RecursoOperativoClient implements RecursoOperativoPort {

    private final WebClient webClient;

    public RecursoOperativoClient(
            WebClient.Builder webClientBuilder,
            @Value("${services.recursos.url:http://localhost:9004}") String recursosUrl) {
        this.webClient = webClientBuilder
                .baseUrl(recursosUrl + "/api/recursos-operativos")
                .build();
    }

    @Override
    public Mono<Void> inhabilitarRecursoPorMantenimiento(String recursoId, String token) {
        log.info("Llamando a vg-ms-recursos para pasar recurso {} a EN_MANTENIMIENTO", recursoId);

        return webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/estado")
                        .queryParam("estado", "EN_MANTENIMIENTO")
                        .build(recursoId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(RecursoOperativoResponseDTO.class)
                .doOnSuccess(dto -> log.info("Recurso {} inhabilitado exitosamente en vg-ms-recursos", recursoId))
                .doOnError(err -> log.error("Error al inhabilitar recurso en vg-ms-recursos: {}", err.getMessage()))
                .then();
    }

    @Override
    public Mono<Void> habilitarRecursoPorAltaOperativa(String recursoId, String token) {
        log.info("Llamando a vg-ms-recursos para retornar recurso {} a DISPONIBLE", recursoId);

        return webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/estado")
                        .queryParam("estado", "DISPONIBLE")
                        .build(recursoId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(RecursoOperativoResponseDTO.class)
                .doOnSuccess(dto -> log.info("Recurso {} habilitado exitosamente a DISPONIBLE", recursoId))
                .doOnError(err -> log.error("Error al habilitar recurso en vg-ms-recursos: {}", err.getMessage()))
                .then();
    }

    @Override
    public Mono<Boolean> validarRecursoExiste(String recursoId, String token) {
        log.info("Validando existencia del recurso {} en vg-ms-recursos", recursoId);

        return webClient.get()
                .uri("/{id}", recursoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(RecursoOperativoResponseDTO.class)
                .map(dto -> dto != null && dto.getId() != null)
                .onErrorResume(e -> {
                    log.warn("El recurso {} no pudo ser verificado o no existe: {}", recursoId, e.getMessage());
                    return Mono.just(false);
                });
    }
}
