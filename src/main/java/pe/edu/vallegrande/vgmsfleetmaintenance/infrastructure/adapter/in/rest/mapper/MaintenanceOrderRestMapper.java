package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.mapper;

import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.MaintenanceOrder;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.TechnicalDischargeReport;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.WorkshopDetail;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.request.AssignWorkshopRequest;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.request.CreateMaintenanceOrderRequest;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.request.TechnicalDischargeRequest;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.request.UpdateMaintenanceOrderRequest;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.response.MaintenanceOrderDetailResponse;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.response.MaintenanceOrderResponse;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.response.TechnicalDischargeResponse;
import pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.adapter.in.rest.dto.response.WorkshopDetailResponse;

/**
 * Primary Inbound REST Mapper for Fleet Maintenance Orders.
 * HEX-04 & STD-040 & STD-063 compliant: isolates domain models from REST boundaries.
 */
public final class MaintenanceOrderRestMapper {

    private MaintenanceOrderRestMapper() {}

    public static MaintenanceOrderResponse toResponse(MaintenanceOrder order) {
        if (order == null) {
            return null;
        }

        return MaintenanceOrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .resourceId(order.getResourceId())
                .resourcePlate(order.getResourcePlate())
                .entityId(order.getEntityId())
                .reportedByDni(order.getReportedByDni())
                .maintenanceType(order.getMaintenanceType())
                .orderStatus(order.getOrderStatus())
                .priority(order.getPriority())
                .failureReason(order.getFailureReason())
                .reportedMileage(order.getReportedMileage())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .status(order.getStatus())
                .build();
    }

    public static MaintenanceOrderDetailResponse toDetailResponse(MaintenanceOrder order) {
        if (order == null) {
            return null;
        }

        return MaintenanceOrderDetailResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .resourceId(order.getResourceId())
                .resourcePlate(order.getResourcePlate())
                .entityId(order.getEntityId())
                .reportedByDni(order.getReportedByDni())
                .incidentId(order.getIncidentId())
                .maintenanceType(order.getMaintenanceType())
                .orderStatus(order.getOrderStatus())
                .priority(order.getPriority())
                .failureReason(order.getFailureReason())
                .reportedMileage(order.getReportedMileage())
                .workshopDetail(toWorkshopDetailResponse(order.getWorkshopDetail()))
                .technicalDischargeReport(toTechnicalDischargeResponse(order.getTechnicalDischargeReport()))
                .remarks(order.getRemarks())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public static WorkshopDetailResponse toWorkshopDetailResponse(WorkshopDetail detail) {
        if (detail == null) {
            return null;
        }

        return WorkshopDetailResponse.builder()
                .workshopName(detail.getWorkshopName())
                .workshopRuc(detail.getWorkshopRuc())
                .mechanicInCharge(detail.getMechanicInCharge())
                .contactPhone(detail.getContactPhone())
                .workshopAddress(detail.getWorkshopAddress())
                .estimatedCost(detail.getEstimatedCost())
                .finalCost(detail.getFinalCost())
                .estimatedDeliveryDate(detail.getEstimatedDeliveryDate())
                .initialDiagnosis(detail.getInitialDiagnosis())
                .build();
    }

    public static TechnicalDischargeResponse toTechnicalDischargeResponse(TechnicalDischargeReport report) {
        if (report == null) {
            return null;
        }

        return TechnicalDischargeResponse.builder()
                .technicalVerdict(report.getTechnicalVerdict())
                .testsPerformed(report.getTestsPerformed())
                .exitMileage(report.getExitMileage())
                .dischargeDate(report.getDischargeDate())
                .approvedByDni(report.getApprovedByDni())
                .remarks(report.getRemarks())
                .mediaUrl(report.getMediaUrl())
                .build();
    }

    public static MaintenanceOrder toDomain(CreateMaintenanceOrderRequest request, String reportedByDni) {
        if (request == null) {
            return null;
        }
        return MaintenanceOrder.create(
                null,
                request.getResourceId(),
                request.getResourcePlate(),
                request.getEntityId(),
                reportedByDni,
                request.getIncidentId(),
                request.getMaintenanceType(),
                request.getPriority(),
                request.getFailureReason(),
                request.getReportedMileage(),
                request.getRemarks()
        );
    }

    public static MaintenanceOrder toDomain(UpdateMaintenanceOrderRequest request) {
        if (request == null) {
            return null;
        }
        WorkshopDetail workshop = request.getWorkshopDetail() != null ? toDomain(request.getWorkshopDetail()) : null;
        return MaintenanceOrder.builder()
                .failureReason(request.getFailureReason())
                .reportedMileage(request.getReportedMileage())
                .priority(request.getPriority())
                .remarks(request.getRemarks())
                .incidentId(request.getIncidentId())
                .workshopDetail(workshop)
                .build();
    }

    public static WorkshopDetail toDomain(AssignWorkshopRequest request) {
        if (request == null) {
            return null;
        }
        return WorkshopDetail.builder()
                .workshopName(request.getWorkshopName())
                .workshopRuc(request.getWorkshopRuc())
                .mechanicInCharge(request.getMechanicInCharge())
                .contactPhone(request.getContactPhone())
                .workshopAddress(request.getWorkshopAddress())
                .estimatedCost(request.getEstimatedCost())
                .estimatedDeliveryDate(request.getEstimatedDeliveryDate())
                .initialDiagnosis(request.getInitialDiagnosis())
                .build();
    }

    public static TechnicalDischargeReport toDomain(TechnicalDischargeRequest request, String approvedByDni) {
        if (request == null) {
            return null;
        }
        return TechnicalDischargeReport.builder()
                .technicalVerdict(request.getTechnicalVerdict())
                .testsPerformed(request.getTestsPerformed())
                .exitMileage(request.getExitMileage())
                .dischargeDate(java.time.LocalDateTime.now())
                .approvedByDni(approvedByDni)
                .remarks(request.getRemarks())
                .build();
    }
}
