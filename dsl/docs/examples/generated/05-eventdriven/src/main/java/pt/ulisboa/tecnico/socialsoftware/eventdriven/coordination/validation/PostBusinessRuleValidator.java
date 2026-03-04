package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.Post;

public class PostBusinessRuleValidator implements ConstraintValidator<ValidPostBusinessRule, Post> {
    
    @Override
    public void initialize(ValidPostBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Post value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Post
        return true;
    }
}