package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.Book;

public class BookBusinessRuleValidator implements ConstraintValidator<ValidBookBusinessRule, Book> {
    
    @Override
    public void initialize(ValidBookBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Book value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Book
        return true;
    }
}