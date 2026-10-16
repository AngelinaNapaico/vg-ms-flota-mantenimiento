package pe.edu.vallegrande.flota.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Value Object inmutable que certifica que el vehículo superó el control de calidad
 * y está en condiciones operativas óptimas para volver al servicio de emergencias.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformeAltaTecnica {

    private String dictamenTecnico;
    private String pruebasRealizadas;
    private Integer kilometrajeSalida;
    private LocalDateTime fechaAlta;
    private String aprobadoPorDni;
    private String observaciones;
}
