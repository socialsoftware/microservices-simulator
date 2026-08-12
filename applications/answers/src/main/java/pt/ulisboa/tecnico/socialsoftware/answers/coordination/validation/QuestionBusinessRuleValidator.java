package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;

public class QuestionBusinessRuleValidator implements ConstraintValidator<ValidQuestionBusinessRule, Question> {
    
    @Override
    public void initialize(ValidQuestionBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Question value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Question
        return true;
    }
}