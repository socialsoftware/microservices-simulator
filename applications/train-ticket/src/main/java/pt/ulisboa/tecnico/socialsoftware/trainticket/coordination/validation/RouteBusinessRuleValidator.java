package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.Route;

public class RouteBusinessRuleValidator implements ConstraintValidator<ValidRouteBusinessRule, Route> {
    
    @Override
    public void initialize(ValidRouteBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Route value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Route
        return true;
    }
}