package pe.edu.vallegrande.flota.infrastructure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.flota.domain.enums.EstadoOrden;
import pe.edu.vallegrande.flota.domain.enums.PrioridadOrden;
import pe.edu.vallegrande.flota.domain.enums.TipoMantenimiento;
import pe.edu.vallegrande.flota.domain.models.DetalleTaller;
import pe.edu.vallegrande.flota.domain.models.InformeAltaTecnica;
import pe.edu.vallegrande.flota.domain.models.OrdenMantenimiento;

import java.time.LocalDateTime;

/**
 * DTO estándar de respuesta para órdenes de mantenimiento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos completos de la orden de mantenimiento")
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
    private String status;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public static OrdenMantenimientoResponse fromDomain(OrdenMantenimiento domain) {
        if (domain == null) return null;
        return OrdenMantenimientoResponse.builder()
                .id(domain.getId())
                .codigoOrden(domain.getCodigoOrden())
                .recursoId(domain.getRecursoId())
                .recursoPlaca(domain.getRecursoPlaca())
                .entidadId(domain.getEntidadId())
                .reportadoPorDni(domain.getReportadoPorDni())
                .incidenteId(domain.getIncidenteId())
                .tipoMantenimiento(domain.getTipoMantenimiento())
                .estadoOrden(domain.getEstadoOrden())
                .prioridad(domain.getPrioridad())
                .motivoAveria(domain.getMotivoAveria())
                .kilometrajeReportado(domain.getKilometrajeReportado())
                .detalleTaller(domain.getDetalleTaller())
                .informeAltaTecnica(domain.getInformeAltaTecnica())
                .observaciones(domain.getObservaciones())
                .status(domain.getStatus())
                .creadoEn(domain.getCreadoEn())
                .actualizadoEn(domain.getActualizadoEn())
                .build();
    }
}
