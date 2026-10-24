package pe.edu.vallegrande.caneteseguro.fleet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.caneteseguro.fleet.domain.exception.DomainException;
import pe.edu.vallegrande.caneteseguro.fleet.domain.vo.MaintenanceType;
import pe.edu.vallegrande.caneteseguro.fleet.domain.vo.OrderPriority;
import pe.edu.vallegrande.caneteseguro.fleet.domain.vo.OrderStatus;

import java.time.LocalDateTime;

/**
 * Aggregate Root del dominio para la gestión del ciclo de vida de mantenimiento de flota.
 * POJO puro 100% en inglés según estándar DatabaseStorage (HEX-01, NOM-01..NOM-03).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceOrder {

    private String id;
    private String orderCode;
    private String resourceId;
    private String resourcePlate;
    private String entityId;
    private String reportedByDni;
    private String incidentId;

    private MaintenanceType maintenanceType;
    private OrderStatus orderStatus;
    private OrderPriority priority;

    private String failureReason;
    private Integer reportedMileage;

    private WorkshopDetail workshopDetail;
    private TechnicalDischargeReport technicalDischargeReport;

    private String remarks;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MaintenanceOrder create(
            String orderCode,
            String resourceId,
            String resourcePlate,
            String entityId,
            String reportedByDni,
            String incidentId,
            MaintenanceType maintenanceType,
            OrderPriority priority,
            String failureReason,
            Integer reportedMileage,
            String remarks
    ) {
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new DomainException("El ID del recurso es obligatorio");
        }
        if (failureReason == null || failureReason.trim().isEmpty()) {
            throw new DomainException("El motivo de avería o requerimiento es obligatorio");
        }
        return MaintenanceOrder.builder()
                .orderCode(orderCode)
                .resourceId(resourceId)
                .resourcePlate(resourcePlate)
                .entityId(entityId)
                .reportedByDni(reportedByDni)
                .incidentId(incidentId)
                .maintenanceType(maintenanceType)
                .priority(priority != null ? priority : OrderPriority.MEDIUM)
                .orderStatus(OrderStatus.REGISTERED)
                .failureReason(failureReason)
                .reportedMileage(reportedMileage)
                .remarks(remarks)
                .status("A")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void assignWorkshop(WorkshopDetail detail) {
        if (this.orderStatus == OrderStatus.COMPLETED || this.orderStatus == OrderStatus.CANCELLED) {
            throw new DomainException("No se puede asignar taller a una orden en estado " + this.orderStatus);
        }
        this.workshopDetail = detail;
        this.orderStatus = OrderStatus.IN_DIAGNOSIS;
        this.updatedAt = LocalDateTime.now();
    }

    public void startRepair() {
        if (this.workshopDetail == null) {
            throw new DomainException("No se puede iniciar reparación sin haber asignado un taller mecánico");
        }
        if (this.orderStatus != OrderStatus.IN_DIAGNOSIS) {
            throw new DomainException("Solo se puede iniciar reparación desde estado IN_DIAGNOSIS. Estado actual: " + this.orderStatus);
        }
        this.orderStatus = OrderStatus.IN_WORKSHOP;
        this.updatedAt = LocalDateTime.now();
    }

    public void passToQualityControl() {
        if (this.orderStatus != OrderStatus.IN_WORKSHOP) {
            throw new DomainException("Solo se puede pasar a control de calidad una orden que esté IN_WORKSHOP. Estado actual: " + this.orderStatus);
        }
        this.orderStatus = OrderStatus.QUALITY_CONTROL;
        this.updatedAt = LocalDateTime.now();
    }

    public void issueTechnicalDischarge(TechnicalDischargeReport report) {
        if (this.orderStatus == OrderStatus.COMPLETED || this.orderStatus == OrderStatus.CANCELLED) {
            throw new DomainException("La orden ya se encuentra cerrada: " + this.orderStatus);
        }
        this.technicalDischargeReport = report;
        this.orderStatus = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        if (this.orderStatus == OrderStatus.COMPLETED) {
            throw new DomainException("No se puede cancelar una orden que ya fue completada (COMPLETED)");
        }
        this.orderStatus = OrderStatus.CANCELLED;
        this.status = "I";
        this.remarks = (this.remarks != null ? this.remarks + " | " : "") + "Cancelada: " + reason;
        this.updatedAt = LocalDateTime.now();
    }
}
