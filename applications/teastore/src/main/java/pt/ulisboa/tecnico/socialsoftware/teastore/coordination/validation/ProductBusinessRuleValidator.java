package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;

public class ProductBusinessRuleValidator implements ConstraintValidator<ValidProductBusinessRule, Product> {
    
    @Override
    public void initialize(ValidProductBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Product value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Product
        return true;
    }
}