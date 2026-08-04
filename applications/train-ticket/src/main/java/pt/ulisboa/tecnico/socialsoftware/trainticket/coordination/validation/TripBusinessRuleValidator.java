package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;

public class TripBusinessRuleValidator implements ConstraintValidator<ValidTripBusinessRule, Trip> {
    
    @Override
    public void initialize(ValidTripBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Trip value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Trip
        return true;
    }
}