package pe.edu.vallegrande.flota.infrastructure.mapper;

import pe.edu.vallegrande.flota.domain.enums.EstadoOrden;
import pe.edu.vallegrande.flota.domain.enums.PrioridadOrden;
import pe.edu.vallegrande.flota.domain.enums.TipoMantenimiento;
import pe.edu.vallegrande.flota.domain.models.OrdenMantenimiento;
import pe.edu.vallegrande.flota.infrastructure.document.BaseDocument;
import pe.edu.vallegrande.flota.infrastructure.document.OrdenMantenimientoDocument;
import pe.edu.vallegrande.flota.infrastructure.dto.request.CreateOrdenMantenimientoRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.response.OrdenMantenimientoDetailResponse;
import pe.edu.vallegrande.flota.infrastructure.dto.response.OrdenMantenimientoResponse;

/**
 * Mapper estático entre el modelo de Dominio puro, el Documento de MongoDB y los DTOs de salida.
 * Según estándar PR241 - Sección 7: Persistencia y Mapeo.
 */
public final class OrdenMantenimientoMapper {

    private OrdenMantenimientoMapper() {}

    public static OrdenMantenimientoDocument toDocument(OrdenMantenimiento domain) {
        if (domain == null) return null;

        BaseDocument.Status docStatus = BaseDocument.Status.A;
        if ("I".equalsIgnoreCase(domain.getStatus())) {
            docStatus = BaseDocument.Status.I;
        } else if ("S".equalsIgnoreCase(domain.getStatus())) {
            docStatus = BaseDocument.Status.S;
        } else if ("P".equalsIgnoreCase(domain.getStatus())) {
            docStatus = BaseDocument.Status.P;
        }

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
                .status(docStatus)
                .creadoEn(domain.getCreadoEn())
                .actualizadoEn(domain.getActualizadoEn())
                .build();
    }

    public static OrdenMantenimiento toDomain(OrdenMantenimientoDocument doc) {
        if (doc == null) return null;

        TipoMantenimiento tipo = null;
        if (doc.getTipoMantenimiento() != null) {
            try {
                tipo = TipoMantenimiento.valueOf(doc.getTipoMantenimiento());
            } catch (IllegalArgumentException ignored) {}
        }

        EstadoOrden estado = null;
        if (doc.getEstadoOrden() != null) {
            try {
                estado = EstadoOrden.valueOf(doc.getEstadoOrden());
            } catch (IllegalArgumentException ignored) {}
        }

        PrioridadOrden prioridad = null;
        if (doc.getPrioridad() != null) {
            try {
                prioridad = PrioridadOrden.valueOf(doc.getPrioridad());
            } catch (IllegalArgumentException ignored) {}
        }

        String statusStr = doc.getStatus() != null ? doc.getStatus().name() : "A";

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
                statusStr,
                doc.getCreadoEn(),
                doc.getActualizadoEn()
        );
    }

    public static OrdenMantenimiento toDomain(CreateOrdenMantenimientoRequest request, String codigoOrden, String reportadoPorDni) {
        if (request == null) return null;
        return OrdenMantenimiento.crear(
                codigoOrden,
                request.getRecursoId(),
                request.getRecursoPlaca(),
                request.getEntidadId(),
                reportadoPorDni,
                request.getIncidenteId(),
                request.getTipoMantenimiento(),
                request.getPrioridad(),
                request.getMotivoAveria(),
                request.getKilometrajeReportado(),
                request.getObservaciones()
        );
    }

    public static OrdenMantenimientoResponse toResponse(OrdenMantenimiento domain) {
        return OrdenMantenimientoResponse.fromDomain(domain);
    }

    public static OrdenMantenimientoDetailResponse toDetailResponse(OrdenMantenimiento domain) {
        return OrdenMantenimientoDetailResponse.fromDomainDetail(domain);
    }
}
