package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.Customer;

public class CustomerBusinessRuleValidator implements ConstraintValidator<ValidCustomerBusinessRule, Customer> {
    
    @Override
    public void initialize(ValidCustomerBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Customer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Customer
        return true;
    }
}