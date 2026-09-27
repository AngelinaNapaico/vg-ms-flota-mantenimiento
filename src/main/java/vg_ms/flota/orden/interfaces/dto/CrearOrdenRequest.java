package vg_ms.flota.orden.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vg_ms.flota.orden.domain.vo.PrioridadOrden;
import vg_ms.flota.orden.domain.vo.TipoMantenimiento;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearOrdenRequest {

    @NotBlank(message = "El ID del recurso operativo es obligatorio")
    private String recursoId;

    private String recursoPlaca;

    @NotBlank(message = "El ID de la entidad a la que pertenece el vehículo es obligatorio")
    private String entidadId;

    private String incidenteId;

    @NotNull(message = "El tipo de mantenimiento es obligatorio (PREVENTIVO, CORRECTIVO_SINIESTRO, URGENCIA_MECANICA)")
    private TipoMantenimiento tipoMantenimiento;

    private PrioridadOrden prioridad;

    @NotBlank(message = "El motivo o falla reportada es obligatorio")
    private String motivoAveria;

    private Integer kilometrajeReportado;

    private String observaciones;
}
