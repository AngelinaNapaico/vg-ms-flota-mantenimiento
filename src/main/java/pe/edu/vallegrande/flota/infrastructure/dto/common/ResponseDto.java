package pe.edu.vallegrande.flota.infrastructure.dto.common;

import java.time.Instant;

/**
 * Wrapper genérico obligatorio para respuestas según estándar PR241 - Sección 4.
 *
 * @param <T> Tipo de datos en la respuesta
 */
public record ResponseDto<T>(
        boolean success,
        T data,
        String message,
        Instant timestamp
) {
    public static <T> ResponseDto<T> success(T data) {
        return new ResponseDto<>(true, data, null, Instant.now());
    }

    public static <T> ResponseDto<T> success(T data, String message) {
        return new ResponseDto<>(true, data, message, Instant.now());
    }

    public static <T> ResponseDto<T> error(String message) {
        return new ResponseDto<>(false, null, message, Instant.now());
    }

    public static ResponseDto<ErrorMessage> error(ErrorMessage errorMessage) {
        return new ResponseDto<>(false, errorMessage, errorMessage.message(), Instant.now());
    }
}
