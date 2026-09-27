package vg_ms.flota.orden.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecursoOperativoResponseDTO {

    private String id;
    private String entidadId;
    private String catalogoId;
    private String identificador;
    private String encargadoNombre;
    private String estadoDisponibilidad;
    private String subestadoOperativo;
    private String estado;
}
