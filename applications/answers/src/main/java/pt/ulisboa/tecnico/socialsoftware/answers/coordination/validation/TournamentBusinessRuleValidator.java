package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;

public class TournamentBusinessRuleValidator implements ConstraintValidator<ValidTournamentBusinessRule, Tournament> {
    
    @Override
    public void initialize(ValidTournamentBusinessRule constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Tournament value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Implement business rule validation logic for Tournament
        return true;
    }
}