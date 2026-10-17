package pe.edu.vallegrande.flota.infrastructure.dto.common;

/**
 * Error de validación de Bean Validation según PR241 - Sección 4.
 */
public record ValidationError(
        String field,
        String rejectedValue,
        String message
) {
    public static ValidationError of(String field, Object rejectedValue, String message) {
        String rejectedStr = rejectedValue != null ? rejectedValue.toString() : "null";
        return new ValidationError(field, rejectedStr, message);
    }
}
