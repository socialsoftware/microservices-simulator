package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.validation;


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
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.Customer;

public class EmailDomainValidator implements ConstraintValidator<ValidEmailDomain, String> {
    
    @Override
    public void initialize(ValidEmailDomain constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Validate email domain
        if (value.isEmpty()) return true; // Let other validators handle empty values
        
        String[] allowedDomains = {"company.com", "organization.org", "domain.net"};
        String domain = value.substring(value.indexOf('@') + 1);
        
        for (String allowedDomain : allowedDomains) {
            if (domain.equalsIgnoreCase(allowedDomain)) {
                return true;
            }
        }
        
        return false;
    }
}