package pe.edu.vallegrande.vgmsfleetmaintenance.infrastructure.exception.custom;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Constraint validator for @ValidEnum annotation.
 */
public class ValidEnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Set<String> acceptedValues;
    private boolean nullable;

    @Override
    public void initialize(ValidEnum annotation) {
        acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
        nullable = annotation.nullable();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return nullable;
        }
        if (value.isBlank()) {
            return false;
        }
        return acceptedValues.contains(value);
    }
}
