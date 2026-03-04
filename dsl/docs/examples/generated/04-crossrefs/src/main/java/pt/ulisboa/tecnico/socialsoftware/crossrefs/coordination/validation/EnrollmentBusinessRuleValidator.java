package pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.Enrollment;

public class EnrollmentBusinessRuleValidator implements ConstraintValidator<ValidEnrollmentBusinessRule, Enrollment> {
    
    @Override
    public void initialize(ValidEnrollmentBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Enrollment value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Enrollment
        return true;
    }
}