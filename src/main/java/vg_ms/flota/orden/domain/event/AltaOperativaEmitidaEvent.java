package vg_ms.flota.orden.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AltaOperativaEmitidaEvent {

    private final String ordenId;
    private final String codigoOrden;
    private final String recursoId;
    private final String recursoPlaca;
    private final LocalDateTime ocurridoEn;
}
