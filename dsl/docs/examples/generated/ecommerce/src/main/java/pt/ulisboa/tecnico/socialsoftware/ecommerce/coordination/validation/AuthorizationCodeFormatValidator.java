package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class AuthorizationCodeFormatValidator implements ConstraintValidator<ValidAuthorizationCodeFormat, String> {
    
    @Override
    public void initialize(ValidAuthorizationCodeFormat constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Validate code format
        if (value.isEmpty()) return true;
        
        return value.matches("^[A-Za-z0-9-]{6,12}$");
    }
}