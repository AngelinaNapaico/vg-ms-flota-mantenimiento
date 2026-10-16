package pe.edu.vallegrande.flota.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Clase base para todos los documentos MongoDB con auditoría estándar.
 * Según estándar PR241 - Sección 7: Persistencia.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseDocument {

    @Id
    private String id;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("created_by")
    private String createdBy;

    @Field("updated_by")
    private String updatedBy;

    @Field("status")
    private Status status;

    public enum Status {
        A, // Activo
        I, // Inactivo
        S, // Suspendido
        P  // Pendiente
    }

    public void initializeAudit(String userId) {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = userId != null ? userId : "SYSTEM";
        this.updatedBy = userId != null ? userId : "SYSTEM";
        if (this.status == null) {
            this.status = Status.A;
        }
    }

    public void updateAudit(String userId) {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = userId != null ? userId : "SYSTEM";
    }

    public void markAsInactive(String userId) {
        this.status = Status.I;
        updateAudit(userId);
    }
}
