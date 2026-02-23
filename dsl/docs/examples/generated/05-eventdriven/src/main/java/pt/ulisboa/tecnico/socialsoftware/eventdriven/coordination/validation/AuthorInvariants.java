package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.validation;


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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.regex.Pattern;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.Author;

/**
 * Invariant validation methods for Author
 */
public class AuthorInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(Author entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(Author entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * Bio cannot be null
     */
    public static void invariantBioNotNull(Author entity) {
        if (entity.getBio() == null) {
            throw new IllegalStateException("Bio cannot be null");
        }
    }

    /**
     * Bio cannot be blank
     */
    public static void invariantBioNotBlank(Author entity) {
        if (entity.getBio() == null || entity.getBio().trim().isEmpty()) {
            throw new IllegalStateException("Bio cannot be blank");
        }
    }

    /**
     * Author aggregate must be in a valid state
     */
    public static void invariantAuthorValid(Author entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}