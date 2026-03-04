package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.Member;

public class MemberBusinessRuleValidator implements ConstraintValidator<ValidMemberBusinessRule, Member> {
    
    @Override
    public void initialize(ValidMemberBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Member value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Member
        return true;
    }
}