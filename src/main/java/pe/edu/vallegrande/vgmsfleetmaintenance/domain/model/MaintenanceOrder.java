package pe.edu.vallegrande.vgmsfleetmaintenance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.MaintenanceType;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderPriority;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderStatus;

import java.time.LocalDateTime;

/**
 * Aggregate Root del dominio para la gestión del ciclo de vida de mantenimiento
 * de flota.
 * POJO puro 100% en inglés según estándar DatabaseStorage (HEX-01,
 * NOM-01..NOM-03).
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
    private Long version;

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
            String remarks) {
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
            throw new DomainException("No es posible asignar taller a una orden de mantenimiento cerrada");
        }
        this.workshopDetail = detail;
        this.orderStatus = OrderStatus.IN_DIAGNOSIS;
        this.updatedAt = LocalDateTime.now();
    }

    public void startRepair() {
        if (this.workshopDetail == null) {
            throw new DomainException("No se puede iniciar reparacion sin haber asignado un taller mecanico");
        }
        if (this.orderStatus != OrderStatus.IN_DIAGNOSIS) {
            throw new DomainException(
                    "Solo se puede iniciar la reparacion cuando la orden se encuentra en diagnostico");
        }
        this.orderStatus = OrderStatus.IN_WORKSHOP;
        this.updatedAt = LocalDateTime.now();
    }

    public void passToQualityControl() {
        if (this.orderStatus != OrderStatus.IN_WORKSHOP) {
            throw new DomainException(
                    "Solo se puede pasar a control de calidad una orden que se encuentra en reparacion");
        }
        this.orderStatus = OrderStatus.IN_QUALITY_CONTROL;
        this.updatedAt = LocalDateTime.now();
    }

    public void issueTechnicalDischarge(TechnicalDischargeReport report) {
        if (this.orderStatus == OrderStatus.COMPLETED || this.orderStatus == OrderStatus.CANCELLED) {
            throw new DomainException("La orden de mantenimiento ya se encuentra cerrada");
        }
        this.technicalDischargeReport = report;
        this.orderStatus = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        if (this.orderStatus == OrderStatus.COMPLETED) {
            throw new DomainException("No se puede cancelar una orden de mantenimiento que ya fue completada");
        }
        this.orderStatus = OrderStatus.CANCELLED;
        this.status = "I";
        this.remarks = (this.remarks != null ? this.remarks + " | " : "") + "Cancelada: " + reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDetails(String failureReason, Integer reportedMileage, OrderPriority priority, String remarks,
            String incidentId) {
        updateDetails(failureReason, reportedMileage, priority, remarks, incidentId, null);
    }

    public void updateDetails(String failureReason, Integer reportedMileage, OrderPriority priority, String remarks,
            String incidentId, WorkshopDetail workshopDetail) {
        if (this.orderStatus == OrderStatus.COMPLETED || this.orderStatus == OrderStatus.CANCELLED) {
            throw new DomainException("No se pueden modificar datos de una orden de mantenimiento cerrada");
        }

        // Si la orden aún está en etapa de registro (REGISTERED), se permite actualizar
        // los datos de ingreso
        if (this.orderStatus == OrderStatus.REGISTERED) {
            if (failureReason != null && !failureReason.trim().isEmpty()) {
                this.failureReason = failureReason;
            }
            if (reportedMileage != null) {
                this.reportedMileage = reportedMileage;
            }
            if (priority != null) {
                this.priority = priority;
            }
            if (incidentId != null) {
                this.incidentId = incidentId;
            }
        }

        if (remarks != null) {
            this.remarks = remarks;
        }

        // En etapas de taller/diagnóstico o posteriores, se actualizan los datos de
        // taller
        if (workshopDetail != null) {
            if (this.workshopDetail == null) {
                this.workshopDetail = workshopDetail;
            } else {
                if (workshopDetail.getWorkshopName() != null && !workshopDetail.getWorkshopName().trim().isEmpty()) {
                    this.workshopDetail.setWorkshopName(workshopDetail.getWorkshopName());
                }
                if (workshopDetail.getWorkshopRuc() != null) {
                    this.workshopDetail.setWorkshopRuc(workshopDetail.getWorkshopRuc());
                }
                if (workshopDetail.getMechanicInCharge() != null) {
                    this.workshopDetail.setMechanicInCharge(workshopDetail.getMechanicInCharge());
                }
                if (workshopDetail.getContactPhone() != null) {
                    this.workshopDetail.setContactPhone(workshopDetail.getContactPhone());
                }
                if (workshopDetail.getWorkshopAddress() != null) {
                    this.workshopDetail.setWorkshopAddress(workshopDetail.getWorkshopAddress());
                }
                if (workshopDetail.getEstimatedCost() != null) {
                    this.workshopDetail.setEstimatedCost(workshopDetail.getEstimatedCost());
                }
                if (workshopDetail.getEstimatedDeliveryDate() != null) {
                    this.workshopDetail.setEstimatedDeliveryDate(workshopDetail.getEstimatedDeliveryDate());
                }
                if (workshopDetail.getInitialDiagnosis() != null) {
                    this.workshopDetail.setInitialDiagnosis(workshopDetail.getInitialDiagnosis());
                }
            }
        }

        this.updatedAt = LocalDateTime.now();
    }
}
