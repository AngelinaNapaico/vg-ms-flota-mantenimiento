package pe.edu.vallegrande.vgmsfleetmaintenance.application.mapper;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper for internal field names to safe, controlled public field names.
 * Prevents internal property/class structure leakage in validation responses 
 */
@UtilityClass
public class FieldNameMapper {

    private static final Map<String, String> FIELD_NAME_MAP = new HashMap<>();

    static {
        // Multi-tenancy & Identification
        FIELD_NAME_MAP.put("entityId", "Entidad");
        FIELD_NAME_MAP.put("municipalityId", "Municipalidad");
        FIELD_NAME_MAP.put("reportedByDni", "DNI del Reportante");
        FIELD_NAME_MAP.put("incidentId", "Incidente");
        FIELD_NAME_MAP.put("resourceId", "Recurso");
        FIELD_NAME_MAP.put("resourcePlate", "Placa del Vehículo");
        FIELD_NAME_MAP.put("orderCode", "Código de Orden");

        // Order classification & details
        FIELD_NAME_MAP.put("maintenanceType", "Tipo de Mantenimiento");
        FIELD_NAME_MAP.put("priority", "Prioridad");
        FIELD_NAME_MAP.put("failureReason", "Motivo de Falla");
        FIELD_NAME_MAP.put("issueDescription", "Descripción del Problema");
        FIELD_NAME_MAP.put("reportedMileage", "Kilometraje");
        FIELD_NAME_MAP.put("remarks", "Observaciones");
        FIELD_NAME_MAP.put("reason", "Motivo");
        FIELD_NAME_MAP.put("notes", "Notas");

        // Workshop assignment
        FIELD_NAME_MAP.put("workshopDetail", "Detalle del Taller");
        FIELD_NAME_MAP.put("workshopName", "Nombre del Taller");
        FIELD_NAME_MAP.put("technicianName", "Técnico Asignado");
        FIELD_NAME_MAP.put("technicianPhone", "Teléfono del Técnico");
        FIELD_NAME_MAP.put("workshopAddress", "Dirección del Taller");
        FIELD_NAME_MAP.put("estimatedDeliveryDate", "Fecha Estimada de Entrega");

        // Technical discharge
        FIELD_NAME_MAP.put("technicalDischargeReport", "Informe de Alta Técnica");
        FIELD_NAME_MAP.put("tasksPerformed", "Tareas Realizadas");
        FIELD_NAME_MAP.put("replacedParts", "Repuestos Reemplazados");
        FIELD_NAME_MAP.put("maintenanceCost", "Costo de Mantenimiento");
        FIELD_NAME_MAP.put("guaranteeMonths", "Meses de Garantía");
        FIELD_NAME_MAP.put("dischargeNotes", "Notas de Alta");
    }

    public static String getPublicFieldName(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return fieldName;
        }
        return FIELD_NAME_MAP.getOrDefault(fieldName, fieldName);
    }
}
