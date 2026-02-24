package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;

public class ExecutionBusinessRuleValidator implements ConstraintValidator<ValidExecutionBusinessRule, Execution> {
    
    @Override
    public void initialize(ValidExecutionBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Execution value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Execution
        return true;
    }
}