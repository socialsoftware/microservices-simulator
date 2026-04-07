package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;

public class PaymentBusinessRuleValidator implements ConstraintValidator<ValidPaymentBusinessRule, Payment> {
    
    @Override
    public void initialize(ValidPaymentBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Payment value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Payment
        return true;
    }
}