package pe.edu.vallegrande.vgmsfleetmaintenance.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Application DTO representing the actor context executing a use case.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActorContextDTO {

    private String actorId;
    private String entityId;
    private String municipalityId;
    private Set<String> roles;
    private boolean isGlobalAdmin;
}
