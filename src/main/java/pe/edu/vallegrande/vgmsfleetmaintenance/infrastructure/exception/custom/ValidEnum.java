package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.exception.custom;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for validating enum values represented as Strings.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEnumValidator.class)
@Documented
public @interface ValidEnum {

    Class<? extends Enum<?>> enumClass();

    String message() default "Value must be one of the allowed enum constants";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    boolean nullable() default true;
}
