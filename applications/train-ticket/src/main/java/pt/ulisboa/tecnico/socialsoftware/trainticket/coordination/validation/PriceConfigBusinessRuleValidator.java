package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;

public class PriceConfigBusinessRuleValidator implements ConstraintValidator<ValidPriceConfigBusinessRule, PriceConfig> {
    
    @Override
    public void initialize(ValidPriceConfigBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(PriceConfig value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for PriceConfig
        return true;
    }
}