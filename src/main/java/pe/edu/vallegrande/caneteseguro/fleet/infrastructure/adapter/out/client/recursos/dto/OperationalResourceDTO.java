package pe.edu.vallegrande.caneteseguro.fleet.infrastructure.adapter.out.client.recursos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Client DTO for vg-ms-recursos response (/api/v1/resources).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationalResourceDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("code")
    private String code;

    @JsonProperty("licensePlate")
    private String licensePlate;

    @JsonProperty("type")
    private String type;

    @JsonProperty("availability")
    private String availability;

    @JsonProperty("status")
    private String status;

    @JsonProperty("responsibleEntityId")
    private String responsibleEntityId;
}
