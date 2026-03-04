package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;

public class LoanBusinessRuleValidator implements ConstraintValidator<ValidLoanBusinessRule, Loan> {
    
    @Override
    public void initialize(ValidLoanBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Loan value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Loan
        return true;
    }
}