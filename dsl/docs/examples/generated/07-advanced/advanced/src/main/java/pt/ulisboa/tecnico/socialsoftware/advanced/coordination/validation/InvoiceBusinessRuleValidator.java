package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.Invoice;

public class InvoiceBusinessRuleValidator implements ConstraintValidator<ValidInvoiceBusinessRule, Invoice> {
    
    @Override
    public void initialize(ValidInvoiceBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Invoice value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Invoice
        return true;
    }
}