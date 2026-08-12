package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;

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