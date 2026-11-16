package pe.edu.vallegrande.vgmsfleetmaintenance.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.MaintenanceType;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderPriority;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderStatus;

import java.time.LocalDateTime;

/**
 * Application response payload returned by use cases.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceOrderResult {

    private String id;
    private String orderCode;
    private String resourceId;
    private String resourcePlate;
    private String entityId;
    private MaintenanceType maintenanceType;
    private OrderStatus orderStatus;
    private OrderPriority priority;
    private String failureReason;
    private Integer reportedMileage;
    private String reportedByDni;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
