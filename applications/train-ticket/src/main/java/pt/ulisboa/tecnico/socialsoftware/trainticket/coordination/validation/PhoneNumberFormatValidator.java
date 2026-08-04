package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class PhoneNumberFormatValidator implements ConstraintValidator<ValidPhoneNumberFormat, String> {
    
    @Override
    public void initialize(ValidPhoneNumberFormat constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Validate phone number format
        if (value.isEmpty()) return true;
        
        String digitsOnly = value.replaceAll("\\D", "");
        
        return digitsOnly.length() >= 10 && digitsOnly.length() <= 15;
    }
}