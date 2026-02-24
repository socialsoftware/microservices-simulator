package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.Category;

public class CategoryBusinessRuleValidator implements ConstraintValidator<ValidCategoryBusinessRule, Category> {
    
    @Override
    public void initialize(ValidCategoryBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Category value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Category
        return true;
    }
}