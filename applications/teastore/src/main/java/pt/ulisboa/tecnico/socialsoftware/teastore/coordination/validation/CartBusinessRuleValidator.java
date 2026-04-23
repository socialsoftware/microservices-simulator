package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.Cart;

public class CartBusinessRuleValidator implements ConstraintValidator<ValidCartBusinessRule, Cart> {
    
    @Override
    public void initialize(ValidCartBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Cart value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Cart
        return true;
    }
}