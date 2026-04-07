package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.aggregate.User;

public class UserBusinessRuleValidator implements ConstraintValidator<ValidUserBusinessRule, User> {
    
    @Override
    public void initialize(ValidUserBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(User value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for User
        return true;
    }
}