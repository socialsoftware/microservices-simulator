package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;

public class TopicBusinessRuleValidator implements ConstraintValidator<ValidTopicBusinessRule, Topic> {
    
    @Override
    public void initialize(ValidTopicBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Topic value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Topic
        return true;
    }
}