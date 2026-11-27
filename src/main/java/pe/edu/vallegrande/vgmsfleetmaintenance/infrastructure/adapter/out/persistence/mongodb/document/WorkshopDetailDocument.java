package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Embedded MongoDB document for workshop detail.
 * Stores monetary amounts using exact BSON Decimal128 representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopDetailDocument {

    @Field("workshopName")
    private String workshopName;

    @Field("workshopRuc")
    private String workshopRuc;

    @Field("mechanicInCharge")
    private String mechanicInCharge;

    @Field("contactPhone")
    private String contactPhone;

    @Field("workshopAddress")
    private String workshopAddress;

    @Field(value = "estimatedCost", targetType = FieldType.DECIMAL128)
    private BigDecimal estimatedCost;

    @Field(value = "finalCost", targetType = FieldType.DECIMAL128)
    private BigDecimal finalCost;

    @Field("estimatedDeliveryDate")
    private LocalDateTime estimatedDeliveryDate;

    @Field("initialDiagnosis")
    private String initialDiagnosis;
}
