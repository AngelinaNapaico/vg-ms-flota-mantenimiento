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
 * Complies with STD-087 and Hexagonal architecture.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceOrderDto {
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
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
