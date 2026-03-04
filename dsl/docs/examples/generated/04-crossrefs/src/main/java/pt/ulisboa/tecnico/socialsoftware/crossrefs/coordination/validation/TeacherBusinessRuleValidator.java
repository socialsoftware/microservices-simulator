package pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.Teacher;

public class TeacherBusinessRuleValidator implements ConstraintValidator<ValidTeacherBusinessRule, Teacher> {
    
    @Override
    public void initialize(ValidTeacherBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Teacher value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Teacher
        return true;
    }
}