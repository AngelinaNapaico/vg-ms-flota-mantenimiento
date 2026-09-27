package vg_ms.flota.orden.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vg_ms.flota.orden.domain.vo.TipoMantenimiento;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrdenMantenimientoCreadaEvent {

    private final String ordenId;
    private final String codigoOrden;
    private final String recursoId;
    private final String recursoPlaca;
    private final String entidadId;
    private final TipoMantenimiento tipoMantenimiento;
    private final LocalDateTime ocurridoEn;
}
