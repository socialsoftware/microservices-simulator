package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.Author;

public class AuthorBusinessRuleValidator implements ConstraintValidator<ValidAuthorBusinessRule, Author> {
    
    @Override
    public void initialize(ValidAuthorBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Author value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Author
        return true;
    }
}