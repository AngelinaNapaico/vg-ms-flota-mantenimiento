package pe.edu.vallegrande.flota.infrastructure.client.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta obtenida de vg-ms-recursos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecursoOperativoDTO {

    private String id;
    private String clave;
    private String identificador;
    private String etiquetaVisual;
    private String tipoRecurso;
    private String categoriaPadre;
    private String entidadId;
    private String estadoDisponibilidad;
    private String estado;
}
