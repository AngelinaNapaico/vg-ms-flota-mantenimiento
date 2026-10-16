package pe.edu.vallegrande.flota.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import pe.edu.vallegrande.flota.domain.models.DetalleTaller;
import pe.edu.vallegrande.flota.domain.models.InformeAltaTecnica;

import java.time.LocalDateTime;

/**
 * Documento de persistencia MongoDB para órdenes de mantenimiento.
 * Mapea a la colección "ordenes_mantenimiento" según estándar PR241.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ordenes_mantenimiento")
public class OrdenMantenimientoDocument extends BaseDocument {

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
