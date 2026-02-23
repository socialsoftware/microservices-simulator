package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;


import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.*;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.regex.Pattern;
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