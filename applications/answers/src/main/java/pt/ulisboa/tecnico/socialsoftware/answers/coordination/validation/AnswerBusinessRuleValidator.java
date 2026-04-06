package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;

public class AnswerBusinessRuleValidator implements ConstraintValidator<ValidAnswerBusinessRule, Answer> {
    
    @Override
    public void initialize(ValidAnswerBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Answer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Answer
        return true;
    }
}