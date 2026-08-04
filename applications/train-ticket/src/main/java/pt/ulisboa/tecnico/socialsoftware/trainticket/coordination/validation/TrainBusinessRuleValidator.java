package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.Train;

public class TrainBusinessRuleValidator implements ConstraintValidator<ValidTrainBusinessRule, Train> {
    
    @Override
    public void initialize(ValidTrainBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Train value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Train
        return true;
    }
}