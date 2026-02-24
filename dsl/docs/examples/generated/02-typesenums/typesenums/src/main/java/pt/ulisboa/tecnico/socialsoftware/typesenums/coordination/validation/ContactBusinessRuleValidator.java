package pt.ulisboa.tecnico.socialsoftware.typesenums.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.Contact;

public class ContactBusinessRuleValidator implements ConstraintValidator<ValidContactBusinessRule, Contact> {
    
    @Override
    public void initialize(ValidContactBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Contact value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Contact
        return true;
    }
}