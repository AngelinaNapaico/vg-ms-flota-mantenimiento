package vg_ms.flota.orden.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vg_ms.flota.orden.domain.aggregate.OrdenMantenimiento;
import vg_ms.flota.orden.domain.vo.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenMantenimientoResponse {

    private String id;
    private String codigoOrden;
    private String recursoId;
    private String recursoPlaca;
    private String entidadId;
    private String reportadoPorDni;
    private String incidenteId;
    private TipoMantenimiento tipoMantenimiento;
    private EstadoOrden estadoOrden;
    private PrioridadOrden prioridad;
    private String motivoAveria;
    private Integer kilometrajeReportado;
    private DetalleTaller detalleTaller;
    private InformeAltaTecnica informeAltaTecnica;
    private String observaciones;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public static OrdenMantenimientoResponse fromDomain(OrdenMantenimiento o) {
        if (o == null) return null;
        return OrdenMantenimientoResponse.builder()
                .id(o.getId())
                .codigoOrden(o.getCodigoOrden())
                .recursoId(o.getRecursoId())
                .recursoPlaca(o.getRecursoPlaca())
                .entidadId(o.getEntidadId())
                .reportadoPorDni(o.getReportadoPorDni())
                .incidenteId(o.getIncidenteId())
                .tipoMantenimiento(o.getTipoMantenimiento())
                .estadoOrden(o.getEstadoOrden())
                .prioridad(o.getPrioridad())
                .motivoAveria(o.getMotivoAveria())
                .kilometrajeReportado(o.getKilometrajeReportado())
                .detalleTaller(o.getDetalleTaller())
                .informeAltaTecnica(o.getInformeAltaTecnica())
                .observaciones(o.getObservaciones())
                .creadoEn(o.getCreadoEn())
                .actualizadoEn(o.getActualizadoEn())
                .build();
    }
}
