package pe.edu.vallegrande.vgmsfleetmaintenance.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.MaintenanceType;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderPriority;

/**
 * Application command DTO for creating a maintenance order.
 * Application command model for initiating a new maintenance order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMaintenanceOrderCommand {
    private String resourceId;
    private String resourcePlate;
    private String entityId;
    private MaintenanceType maintenanceType;
    private OrderPriority priority;
    private String failureReason;
    private Integer reportedMileage;
    private String remarks;
    private String incidentId;
}
