package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document storing audit event records for sensitive maintenance operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "maintenance_order_audits")
public class AuditLogDocument {

    @Id
    private String id;
    @Indexed
    private Instant timestamp;
    private String action;
    private String orderCode;
    private String resourceId;
    private String actor;
    @Indexed
    private String tenant;
    private String result;
    @Indexed
    private String correlationId;
    private String errorDetails;
}
