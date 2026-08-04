package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.Contacts;

public class ContactsBusinessRuleValidator implements ConstraintValidator<ValidContactsBusinessRule, Contacts> {
    
    @Override
    public void initialize(ValidContactsBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Contacts value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Contacts
        return true;
    }
}