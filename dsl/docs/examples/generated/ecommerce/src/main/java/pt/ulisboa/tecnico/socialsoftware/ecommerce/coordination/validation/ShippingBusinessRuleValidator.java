package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;

public class ShippingBusinessRuleValidator implements ConstraintValidator<ValidShippingBusinessRule, Shipping> {
    
    @Override
    public void initialize(ValidShippingBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Shipping value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Shipping
        return true;
    }
}