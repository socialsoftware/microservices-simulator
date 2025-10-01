package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.validation.validators;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;


public class StartDateEndDateRangeValidator implements ConstraintValidator<ValidStartDateEndDateRange, Execution> {
    
    @Override
    public void initialize(ValidStartDateEndDateRange constraintAnnotation) {
        // Initialize validator if needed
    }
    
    @Override
    public boolean isValid(Execution value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Validate date range
        java.lang.reflect.Field startField = null;
        java.lang.reflect.Field endField = null;
        
        try {
            startField = value.getClass().getDeclaredField("startDate");
            endField = value.getClass().getDeclaredField("endDate");
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
            
            // Add more date type comparisons as needed
            return true;
            
        } catch (Exception e) {
            return false; // Validation failed due to reflection issues
        }
    }
}