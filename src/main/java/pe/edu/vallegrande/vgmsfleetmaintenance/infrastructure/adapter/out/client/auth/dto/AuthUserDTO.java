package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Data Transfer Object representing authenticated user profile and roles
 * retrieved from identity and authorization tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthUserDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("entityId")
    private String entityId;

    @JsonProperty("municipalityId")
    private String municipalityId;

    @JsonProperty("roles")
    private Set<String> roles;
}
