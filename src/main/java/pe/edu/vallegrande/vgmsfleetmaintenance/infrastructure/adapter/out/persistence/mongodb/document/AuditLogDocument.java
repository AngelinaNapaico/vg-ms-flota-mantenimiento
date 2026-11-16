package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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
    private Instant timestamp;
    private String action;
    private String orderCode;
    private String resourceId;
    private String actor;
    private String tenant;
    private String result;
    private String correlationId;
    private String errorDetails;
}
