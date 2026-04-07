package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;

public class OrderBusinessRuleValidator implements ConstraintValidator<ValidOrderBusinessRule, Order> {
    
    @Override
    public void initialize(ValidOrderBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Order value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Order
        return true;
    }
}