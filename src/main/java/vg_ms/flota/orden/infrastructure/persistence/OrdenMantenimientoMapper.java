package vg_ms.flota.orden.infrastructure.persistence;

import vg_ms.flota.orden.domain.aggregate.OrdenMantenimiento;
import vg_ms.flota.orden.domain.vo.EstadoOrden;
import vg_ms.flota.orden.domain.vo.PrioridadOrden;
import vg_ms.flota.orden.domain.vo.TipoMantenimiento;

public class OrdenMantenimientoMapper {

    private OrdenMantenimientoMapper() {}

    public static OrdenMantenimientoDocument toDocument(OrdenMantenimiento domain) {
        if (domain == null) return null;

        return OrdenMantenimientoDocument.builder()
                .id(domain.getId())
                .codigoOrden(domain.getCodigoOrden())
                .recursoId(domain.getRecursoId())
                .recursoPlaca(domain.getRecursoPlaca())
                .entidadId(domain.getEntidadId())
                .reportadoPorDni(domain.getReportadoPorDni())
                .incidenteId(domain.getIncidenteId())
                .tipoMantenimiento(domain.getTipoMantenimiento() != null ? domain.getTipoMantenimiento().name() : null)
                .estadoOrden(domain.getEstadoOrden() != null ? domain.getEstadoOrden().name() : null)
                .prioridad(domain.getPrioridad() != null ? domain.getPrioridad().name() : null)
                .motivoAveria(domain.getMotivoAveria())
                .kilometrajeReportado(domain.getKilometrajeReportado())
                .detalleTaller(domain.getDetalleTaller())
                .informeAltaTecnica(domain.getInformeAltaTecnica())
                .observaciones(domain.getObservaciones())
                .creadoEn(domain.getCreadoEn())
                .actualizadoEn(domain.getActualizadoEn())
                .build();
    }

    public static OrdenMantenimiento toDomain(OrdenMantenimientoDocument doc) {
        if (doc == null) return null;

        TipoMantenimiento tipo = doc.getTipoMantenimiento() != null
                ? TipoMantenimiento.valueOf(doc.getTipoMantenimiento())
                : TipoMantenimiento.PREVENTIVO;

        EstadoOrden estado = doc.getEstadoOrden() != null
                ? EstadoOrden.valueOf(doc.getEstadoOrden())
                : EstadoOrden.REGISTRADA;

        PrioridadOrden prioridad = doc.getPrioridad() != null
                ? PrioridadOrden.valueOf(doc.getPrioridad())
                : PrioridadOrden.MEDIA;

        return OrdenMantenimiento.reconstruir(
                doc.getId(),
                doc.getCodigoOrden(),
                doc.getRecursoId(),
                doc.getRecursoPlaca(),
                doc.getEntidadId(),
                doc.getReportadoPorDni(),
                doc.getIncidenteId(),
                tipo,
                estado,
                prioridad,
                doc.getMotivoAveria(),
                doc.getKilometrajeReportado(),
                doc.getDetalleTaller(),
                doc.getInformeAltaTecnica(),
                doc.getObservaciones(),
                doc.getCreadoEn(),
                doc.getActualizadoEn()
        );
    }
}
