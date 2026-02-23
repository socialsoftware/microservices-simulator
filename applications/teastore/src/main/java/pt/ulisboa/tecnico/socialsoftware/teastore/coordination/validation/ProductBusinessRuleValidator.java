package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;


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
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;

public class ProductBusinessRuleValidator implements ConstraintValidator<ValidProductBusinessRule, Product> {
    
    @Override
    public void initialize(ValidProductBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Product value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Product
        return true;
    }
}