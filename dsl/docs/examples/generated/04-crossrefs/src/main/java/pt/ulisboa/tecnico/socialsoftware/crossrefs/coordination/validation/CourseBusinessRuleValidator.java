package pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.Course;

public class CourseBusinessRuleValidator implements ConstraintValidator<ValidCourseBusinessRule, Course> {
    
    @Override
    public void initialize(ValidCourseBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Course value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Course
        return true;
    }
}