package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


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