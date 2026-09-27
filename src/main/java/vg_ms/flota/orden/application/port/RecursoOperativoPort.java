package vg_ms.flota.orden.application.port;

import reactor.core.publisher.Mono;

public interface RecursoOperativoPort {

    /**
     * Inhabilita el recurso en vg-ms-recursos cambiándolo a estado EN_MANTENIMIENTO.
     */
    Mono<Void> inhabilitarRecursoPorMantenimiento(String recursoId, String token);

    /**
     * Habilita nuevamente el recurso en vg-ms-recursos cambiándolo a DISPONIBLE al emitirse el alta técnica.
     */
    Mono<Void> habilitarRecursoPorAltaOperativa(String recursoId, String token);

    /**
     * Verifica la existencia y datos básicos del recurso operativo en vg-ms-recursos.
     */
    Mono<Boolean> validarRecursoExiste(String recursoId, String token);
}
