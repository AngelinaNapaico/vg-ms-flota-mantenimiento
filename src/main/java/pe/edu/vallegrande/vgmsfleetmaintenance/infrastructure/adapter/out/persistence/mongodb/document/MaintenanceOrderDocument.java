package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * MongoDB document entity representing the "maintenance_orders" collection.
 * Includes tenant compound index, optimistic locking versioning,
 * and lifecycle tracking fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "maintenance_orders")
@CompoundIndexes({
        @CompoundIndex(name = "idx_entity_order_code", def = "{'entityId': 1, 'orderCode': 1}", unique = true),
        @CompoundIndex(name = "idx_entity_resource", def = "{'entityId': 1, 'resourceId': 1}"),
        @CompoundIndex(name = "idx_entity_status", def = "{'entityId': 1, 'orderStatus': 1}"),
        @CompoundIndex(name = "idx_entity_resource_status", def = "{'entityId': 1, 'resourceId': 1, 'orderStatus': 1}"),
        @CompoundIndex(name = "idx_unique_active_resource_order", def = "{'entityId': 1, 'resourceId': 1, 'active': 1}", partialFilter = "{'active': true}", unique = true)
})
public class MaintenanceOrderDocument {

    @Id
    private String id;

    @Field("orderCode")
    private String orderCode;

    @Field("resourceId")
    private String resourceId;

    @Field("resourcePlate")
    private String resourcePlate;

    @Indexed
    @Field("entityId")
    private String entityId;

    @Field("reportedByDni")
    private String reportedByDni;

    @Field("incidentId")
    private String incidentId;

    @Field("maintenanceType")
    private String maintenanceType;

    @Field("orderStatus")
    private String orderStatus;

    @Field("priority")
    private String priority;

    @Field("failureReason")
    private String failureReason;

    @Field("reportedMileage")
    private Integer reportedMileage;

    @Field("workshopDetail")
    private WorkshopDetailDocument workshopDetail;

    @Field("technicalDischargeReport")
    private TechnicalDischargeReportDocument technicalDischargeReport;

    @Field("remarks")
    private String remarks;

    @Field("status")
    private String status;

    @Field("active")
    private Boolean active;

    @Version
    @Field("version")
    private Long version;

    @CreatedDate
    @Field("createdAt")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private LocalDateTime updatedAt;
}
