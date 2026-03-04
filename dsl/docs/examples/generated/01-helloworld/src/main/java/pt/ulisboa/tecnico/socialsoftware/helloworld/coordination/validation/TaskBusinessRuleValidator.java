package pt.ulisboa.tecnico.socialsoftware.helloworld.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.Task;

public class TaskBusinessRuleValidator implements ConstraintValidator<ValidTaskBusinessRule, Task> {
    
    @Override
    public void initialize(ValidTaskBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Task value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Task
        return true;
    }
}