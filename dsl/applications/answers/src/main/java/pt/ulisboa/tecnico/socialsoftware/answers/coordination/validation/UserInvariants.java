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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.regex.Pattern;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.User;

/**
 * Invariant validation methods for User
 */
public class UserInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(User entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(User entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * Username cannot be null
     */
    public static void invariantUsernameNotNull(User entity) {
        if (entity.getUsername() == null) {
            throw new IllegalStateException("Username cannot be null");
        }
    }

    /**
     * Username cannot be blank
     */
    public static void invariantUsernameNotBlank(User entity) {
        if (entity.getUsername() == null || entity.getUsername().trim().isEmpty()) {
            throw new IllegalStateException("Username cannot be blank");
        }
    }

    /**
     * Role cannot be null
     */
    public static void invariantRoleNotNull(User entity) {
        if (entity.getRole() == null) {
            throw new IllegalStateException("Role cannot be null");
        }
    }

    /**
     * Active cannot be null
     */
    public static void invariantActiveNotNull(User entity) {
        if (entity.getActive() == null) {
            throw new IllegalStateException("Active cannot be null");
        }
    }

    /**
     * User aggregate must be in a valid state
     */
    public static void invariantUserValid(User entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}