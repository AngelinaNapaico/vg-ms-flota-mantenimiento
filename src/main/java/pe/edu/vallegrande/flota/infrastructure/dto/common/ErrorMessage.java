package pe.edu.vallegrande.flota.infrastructure.dto.common;

/**
 * Mensaje de error estándar según PR241 - Sección 4.
 */
public record ErrorMessage(
        String code,
        String message
) {
    public static ErrorMessage of(String code, String message) {
        return new ErrorMessage(code, message);
    }
}
