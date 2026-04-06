package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;

public class QuizBusinessRuleValidator implements ConstraintValidator<ValidQuizBusinessRule, Quiz> {
    
    @Override
    public void initialize(ValidQuizBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Quiz value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Quiz
        return true;
    }
}