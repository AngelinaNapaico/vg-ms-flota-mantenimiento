package pe.edu.vallegrande.vgmsfleetmaintenance.application.mapper;

import lombok.experimental.UtilityClass;

/**
 * Mapper from Bean Validation internal messages to safe, controlled public messages.
 * Prevents internal configuration and regex pattern leakage 
 */
@UtilityClass
public class ValidationMessageMapper {

    public static String mapValidationMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.isBlank()) {
            return "Este campo tiene un error de validación";
        }

        String message = originalMessage.trim();

        if (isRequired(message)) {
            return "Este campo es obligatorio";
        }
        if (isSize(message)) {
            return "Este campo tiene una longitud no válida";
        }
        if (isEnum(message)) {
            return "Este campo tiene un valor no válido";
        }
        if (isEmail(message)) {
            return "El formato del correo electrónico no es válido";
        }
        if (isNumber(message)) {
            return "El valor debe ser un número válido";
        }
        if (isPattern(message)) {
            return "El formato del valor no es válido";
        }
        if (isRange(message)) {
            return "El valor está fuera del rango permitido";
        }
        if (isBoolean(message)) {
            return "Este campo debe ser verdadero o falso";
        }

        return "Este campo tiene un error de validación";
    }

    private static boolean isRequired(String message) {
        String lower = message.toLowerCase();
        return lower.contains("obligatorio")
                || lower.contains("required")
                || lower.contains("must not be blank")
                || lower.contains("must not be null")
                || lower.contains("must not be empty");
    }

    private static boolean isSize(String message) {
        String lower = message.toLowerCase();
        return lower.contains("no puede exceder")
                || lower.contains("caracteres")
                || lower.contains("tamaño")
                || lower.contains("size")
                || lower.contains("length")
                || lower.contains("must be between");
    }

    private static boolean isEnum(String message) {
        String lower = message.toLowerCase();
        return lower.contains("enum")
                || lower.contains("estado")
                || lower.contains("status")
                || lower.contains("prioridad")
                || lower.contains("priority")
                || lower.contains("tipo")
                || lower.contains("type")
                || lower.contains("válido")
                || lower.contains("must be one of");
    }

    private static boolean isEmail(String message) {
        String lower = message.toLowerCase();
        return lower.contains("correo") || lower.contains("email") || lower.contains("@");
    }

    private static boolean isNumber(String message) {
        String lower = message.toLowerCase();
        return lower.contains("número") || lower.contains("numérico") || lower.contains("number");
    }

    private static boolean isPattern(String message) {
        String lower = message.toLowerCase();
        return lower.contains("formato") || lower.contains("patrón") || lower.contains("pattern");
    }

    private static boolean isRange(String message) {
        String lower = message.toLowerCase();
        return lower.contains("rango") || lower.contains("min") || lower.contains("max") || lower.contains("mínimo") || lower.contains("máximo");
    }

    private static boolean isBoolean(String message) {
        String lower = message.toLowerCase();
        return lower.contains("booleano") || lower.contains("boolean") || lower.contains("verdadero") || lower.contains("falso");
    }
}
