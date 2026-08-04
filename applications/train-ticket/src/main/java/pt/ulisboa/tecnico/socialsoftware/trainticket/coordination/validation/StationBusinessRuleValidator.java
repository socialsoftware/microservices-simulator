package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station;

public class StationBusinessRuleValidator implements ConstraintValidator<ValidStationBusinessRule, Station> {
    
    @Override
    public void initialize(ValidStationBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Station value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Station
        return true;
    }
}