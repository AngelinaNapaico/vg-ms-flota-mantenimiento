package pe.edu.vallegrande.vgmsfleetmaintenance.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.MaintenanceType;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderPriority;

/**
 * Application command for initiating a maintenance order.
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
    private String observations;
    private String reportedByDni;
}
