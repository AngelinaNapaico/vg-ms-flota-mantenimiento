package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.client.auth.dto;

/**
 * Immutable DTO representing the normalized identity and organizational scope
 * extracted from an authenticated security principal.
 */
public record AuthUserDTO(
        String username,
        String entityId,
        boolean globalAccess,
        boolean entityWideAccess) {
}
