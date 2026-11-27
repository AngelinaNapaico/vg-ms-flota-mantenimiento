package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.mapper;

import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.MaintenanceOrder;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.TechnicalDischargeReport;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.WorkshopDetail;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.MaintenanceType;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderPriority;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderStatus;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.MaintenanceOrderDocument;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.TechnicalDischargeReportDocument;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.out.persistence.mongodb.document.WorkshopDetailDocument;

/**
 * Bidirectional mapper between Domain Model and Persistence Document for MongoDB.
 * Translates between domain entity representations and database document models.
 */
public final class MaintenanceOrderDocumentMapper {

    private MaintenanceOrderDocumentMapper() {}

    public static MaintenanceOrder toDomain(MaintenanceOrderDocument document) {
        if (document == null) {
            return null;
        }

        MaintenanceType type = null;
        if (document.getMaintenanceType() != null) {
            String t = document.getMaintenanceType();
            switch (t) {
                case "PREVENTIVE", "PREVENTIVO" -> type = MaintenanceType.PREVENTIVE;
                case "CORRECTIVE", "CORRECTIVO" -> type = MaintenanceType.CORRECTIVE;
                case "CORRECTIVE_ACCIDENT", "CORRECTIVO_ACCIDENTE" -> type = MaintenanceType.CORRECTIVE_ACCIDENT;
                case "REGULATORY_INSPECTION", "STATUTORY_INSPECTION", "INSPECCION_REGLAMENTARIA" -> type = MaintenanceType.STATUTORY_INSPECTION;
                default -> {
                    try {
                        type = MaintenanceType.valueOf(t);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        OrderStatus status = null;
        if (document.getOrderStatus() != null) {
            String s = document.getOrderStatus();
            switch (s) {
                case "REGISTERED", "REGISTRADA" -> status = OrderStatus.REGISTERED;
                case "IN_DIAGNOSIS", "EN_DIAGNOSTICO" -> status = OrderStatus.IN_DIAGNOSIS;
                case "IN_WORKSHOP", "IN_REPAIR", "EN_TALLER" -> status = OrderStatus.IN_WORKSHOP;
                case "QUALITY_CONTROL", "IN_QUALITY_CONTROL", "EN_CONTROL_CALIDAD" -> status = OrderStatus.IN_QUALITY_CONTROL;
                case "COMPLETED", "FINALIZADA" -> status = OrderStatus.COMPLETED;
                case "CANCELLED", "CANCELADA" -> status = OrderStatus.CANCELLED;
                default -> {
                    try {
                        status = OrderStatus.valueOf(s);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        OrderPriority priority = null;
        if (document.getPriority() != null) {
            String p = document.getPriority();
            switch (p) {
                case "LOW", "BAJA" -> priority = OrderPriority.LOW;
                case "MEDIUM", "MEDIA" -> priority = OrderPriority.MEDIUM;
                case "HIGH", "ALTA" -> priority = OrderPriority.HIGH;
                case "CRITICAL", "CRITICA" -> priority = OrderPriority.CRITICAL;
                default -> {
                    try {
                        priority = OrderPriority.valueOf(p);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        return MaintenanceOrder.builder()
                .id(document.getId())
                .orderCode(document.getOrderCode())
                .resourceId(document.getResourceId())
                .resourcePlate(document.getResourcePlate())
                .entityId(document.getEntityId())
                .reportedByDni(document.getReportedByDni())
                .incidentId(document.getIncidentId())
                .maintenanceType(type)
                .orderStatus(status)
                .priority(priority)
                .failureReason(document.getFailureReason())
                .reportedMileage(document.getReportedMileage())
                .workshopDetail(toDomain(document.getWorkshopDetail()))
                .technicalDischargeReport(toDomain(document.getTechnicalDischargeReport()))
                .remarks(document.getRemarks())
                .status(document.getStatus())
                .active(document.getActive())
                .version(document.getVersion())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    public static MaintenanceOrderDocument toDocument(MaintenanceOrder order) {
        if (order == null) {
            return null;
        }

        return MaintenanceOrderDocument.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .resourceId(order.getResourceId())
                .resourcePlate(order.getResourcePlate())
                .entityId(order.getEntityId())
                .reportedByDni(order.getReportedByDni())
                .incidentId(order.getIncidentId())
                .maintenanceType(order.getMaintenanceType() != null ? order.getMaintenanceType().name() : null)
                .orderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null)
                .priority(order.getPriority() != null ? order.getPriority().name() : null)
                .failureReason(order.getFailureReason())
                .reportedMileage(order.getReportedMileage())
                .workshopDetail(toDocument(order.getWorkshopDetail()))
                .technicalDischargeReport(toDocument(order.getTechnicalDischargeReport()))
                .remarks(order.getRemarks())
                .status(order.getStatus())
                .active(resolveActiveFlag(order))
                .version(order.getVersion())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private static Boolean resolveActiveFlag(MaintenanceOrder order) {
        if (order.getOrderStatus() == OrderStatus.COMPLETED
                || order.getOrderStatus() == OrderStatus.CANCELLED
                || "I".equalsIgnoreCase(order.getStatus())) {
            return Boolean.FALSE;
        }
        return order.getActive() != null ? order.getActive() : Boolean.TRUE;
    }

    public static WorkshopDetail toDomain(WorkshopDetailDocument document) {
        if (document == null) {
            return null;
        }

        return WorkshopDetail.builder()
                .workshopName(document.getWorkshopName())
                .workshopRuc(document.getWorkshopRuc())
                .mechanicInCharge(document.getMechanicInCharge())
                .contactPhone(document.getContactPhone())
                .workshopAddress(document.getWorkshopAddress())
                .estimatedCost(document.getEstimatedCost())
                .finalCost(document.getFinalCost())
                .estimatedDeliveryDate(document.getEstimatedDeliveryDate())
                .initialDiagnosis(document.getInitialDiagnosis())
                .build();
    }

    public static WorkshopDetailDocument toDocument(WorkshopDetail domain) {
        if (domain == null) {
            return null;
        }

        return WorkshopDetailDocument.builder()
                .workshopName(domain.getWorkshopName())
                .workshopRuc(domain.getWorkshopRuc())
                .mechanicInCharge(domain.getMechanicInCharge())
                .contactPhone(domain.getContactPhone())
                .workshopAddress(domain.getWorkshopAddress())
                .estimatedCost(WorkshopDetail.normalizeCost(domain.getEstimatedCost(), "Estimated cost"))
                .finalCost(WorkshopDetail.normalizeCost(domain.getFinalCost(), "Final cost"))
                .estimatedDeliveryDate(domain.getEstimatedDeliveryDate())
                .initialDiagnosis(domain.getInitialDiagnosis())
                .build();
    }

    public static TechnicalDischargeReport toDomain(TechnicalDischargeReportDocument document) {
        if (document == null) {
            return null;
        }

        return TechnicalDischargeReport.builder()
                .technicalVerdict(document.getTechnicalVerdict())
                .testsPerformed(document.getTestsPerformed())
                .exitMileage(document.getExitMileage())
                .dischargeDate(document.getDischargeDate())
                .approvedByDni(document.getApprovedByDni())
                .remarks(document.getRemarks())
                .mediaUrl(document.getMediaUrl())
                .build();
    }

    public static TechnicalDischargeReportDocument toDocument(TechnicalDischargeReport domain) {
        if (domain == null) {
            return null;
        }

        return TechnicalDischargeReportDocument.builder()
                .technicalVerdict(domain.getTechnicalVerdict())
                .testsPerformed(domain.getTestsPerformed())
                .exitMileage(domain.getExitMileage())
                .dischargeDate(domain.getDischargeDate())
                .approvedByDni(domain.getApprovedByDni())
                .remarks(domain.getRemarks())
                .mediaUrl(domain.getMediaUrl())
                .build();
    }
}
