package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.validation.validators;


import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.*;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.regex.Pattern;

public class StartTimeEndTimeRangeValidator implements ConstraintValidator<ValidStartTimeEndTimeRange, Tournament> {
    
    @Override
    public void initialize(ValidStartTimeEndTimeRange constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Tournament value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Validate date range
        java.lang.reflect.Field startField = null;
        java.lang.reflect.Field endField = null;
        
        try {
            startField = value.getClass().getDeclaredField("startTime");
            endField = value.getClass().getDeclaredField("endTime");
            startField.setAccessible(true);
            endField.setAccessible(true);
            
            Object startValue = startField.get(value);
            Object endValue = endField.get(value);
            
            if (startValue == null || endValue == null) {
                return true; // Let other validators handle null values
            }
            
            if (startValue instanceof java.time.LocalDateTime && endValue instanceof java.time.LocalDateTime) {
                return ((java.time.LocalDateTime) startValue).isBefore((java.time.LocalDateTime) endValue);
            }
            
            return true;
            
        } catch (Exception e) {
            return false; // Validation failed due to reflection issues
        }
    }
}