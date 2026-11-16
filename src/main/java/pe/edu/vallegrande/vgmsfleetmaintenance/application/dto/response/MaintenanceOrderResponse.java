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
 * Application response DTO for maintenance order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceOrderResponse {
    private String id;
    private String orderCode;
    private String resourceId;
    private String resourcePlate;
    private String entityId;
    private OrderStatus orderStatus;
    private MaintenanceType maintenanceType;
    private OrderPriority priority;
    private String failureReason;
    private Integer reportedMileage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
