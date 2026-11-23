package pe.edu.vallegrande.vgmsfleetmaintenance.application.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("FieldNameMapper - Controlled Public Field Labels")
class FieldNameMapperTest {

    @Test
    @DisplayName("Should map all workshop and discharge validated fields to controlled public labels")
    void shouldMapAllValidatedFieldsToControlledLabels() {
        assertEquals("RUC del Taller", FieldNameMapper.getPublicFieldName("workshopRuc"));
        assertEquals("Mecánico Responsable", FieldNameMapper.getPublicFieldName("mechanicInCharge"));
        assertEquals("Teléfono de Contacto", FieldNameMapper.getPublicFieldName("contactPhone"));
        assertEquals("Costo Estimado", FieldNameMapper.getPublicFieldName("estimatedCost"));
        assertEquals("Diagnóstico Inicial", FieldNameMapper.getPublicFieldName("initialDiagnosis"));
        assertEquals("Dictamen Técnico", FieldNameMapper.getPublicFieldName("technicalVerdict"));
        assertEquals("Pruebas Realizadas", FieldNameMapper.getPublicFieldName("testsPerformed"));
        assertEquals("Kilometraje de Salida", FieldNameMapper.getPublicFieldName("exitMileage"));
        assertEquals("Costo Final", FieldNameMapper.getPublicFieldName("finalCost"));
    }

    @Test
    @DisplayName("Should resolve nested DTO field paths without exposing parent attribute names")
    void shouldResolveNestedFieldPaths() {
        assertEquals("Mecánico Responsable", FieldNameMapper.getPublicFieldName("workshopDetail.mechanicInCharge"));
        assertEquals("Costo Estimado", FieldNameMapper.getPublicFieldName("workshopDetail.estimatedCost"));
    }

    @Test
    @DisplayName("Should return generic label for unknown, null or blank internal field names")
    void shouldNeverExposeUnknownInternalFieldNames() {
        assertEquals("Campo", FieldNameMapper.getPublicFieldName("internalSecretProperty"));
        assertNotEquals("internalSecretProperty", FieldNameMapper.getPublicFieldName("internalSecretProperty"));
        assertEquals("Campo", FieldNameMapper.getPublicFieldName(null));
        assertEquals("Campo", FieldNameMapper.getPublicFieldName("   "));
    }
}
