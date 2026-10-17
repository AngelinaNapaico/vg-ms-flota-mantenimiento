package pe.edu.vallegrande.flota.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.flota.domain.models.OrdenMantenimiento;

/**
 * DTO para respuesta detallada de una orden de mantenimiento según estándar PR241.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Detalle exhaustivo de la orden de mantenimiento")
public class OrdenMantenimientoDetailResponse extends OrdenMantenimientoResponse {

    public static OrdenMantenimientoDetailResponse fromDomainDetail(OrdenMantenimiento domain) {
        if (domain == null) return null;
        OrdenMantenimientoDetailResponse detail = new OrdenMantenimientoDetailResponse();
        detail.setId(domain.getId());
        detail.setCodigoOrden(domain.getCodigoOrden());
        detail.setRecursoId(domain.getRecursoId());
        detail.setRecursoPlaca(domain.getRecursoPlaca());
        detail.setEntidadId(domain.getEntidadId());
        detail.setReportadoPorDni(domain.getReportadoPorDni());
        detail.setIncidenteId(domain.getIncidenteId());
        detail.setTipoMantenimiento(domain.getTipoMantenimiento());
        detail.setEstadoOrden(domain.getEstadoOrden());
        detail.setPrioridad(domain.getPrioridad());
        detail.setMotivoAveria(domain.getMotivoAveria());
        detail.setKilometrajeReportado(domain.getKilometrajeReportado());
        detail.setDetalleTaller(domain.getDetalleTaller());
        detail.setInformeAltaTecnica(domain.getInformeAltaTecnica());
        detail.setObservaciones(domain.getObservaciones());
        detail.setStatus(domain.getStatus());
        detail.setCreadoEn(domain.getCreadoEn());
        detail.setActualizadoEn(domain.getActualizadoEn());
        return detail;
    }
}
