package vg_ms.flota.orden.infrastructure.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import vg_ms.flota.orden.domain.vo.DetalleTaller;
import vg_ms.flota.orden.domain.vo.InformeAltaTecnica;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ordenes_mantenimiento")
public class OrdenMantenimientoDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("codigo_orden")
    private String codigoOrden;

    @Indexed
    @Field("recurso_id")
    private String recursoId;

    @Field("recurso_placa")
    private String recursoPlaca;

    @Indexed
    @Field("entidad_id")
    private String entidadId;

    @Field("reportado_por_dni")
    private String reportadoPorDni;

    @Field("incidente_id")
    private String incidenteId;

    @Field("tipo_mantenimiento")
    private String tipoMantenimiento;

    @Indexed
    @Field("estado_orden")
    private String estadoOrden;

    @Field("prioridad")
    private String prioridad;

    @Field("motivo_averia")
    private String motivoAveria;

    @Field("kilometraje_reportado")
    private Integer kilometrajeReportado;

    @Field("detalle_taller")
    private DetalleTaller detalleTaller;

    @Field("informe_alta_tecnica")
    private InformeAltaTecnica informeAltaTecnica;

    @Field("observaciones")
    private String observaciones;

    @Field("creado_en")
    private LocalDateTime creadoEn;

    @Field("actualizado_en")
    private LocalDateTime actualizadoEn;
}
