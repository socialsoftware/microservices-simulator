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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.regex.Pattern;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.aggregate.User;

/**
 * Invariant validation methods for User
 */
public class UserInvariants {

    /**
     * UserName cannot be null
     */
    public static void invariantUserNameNotNull(User entity) {
        if (entity.getUserName() == null) {
            throw new IllegalStateException("UserName cannot be null");
        }
    }

    /**
     * UserName cannot be blank
     */
    public static void invariantUserNameNotBlank(User entity) {
        if (entity.getUserName() == null || entity.getUserName().trim().isEmpty()) {
            throw new IllegalStateException("UserName cannot be blank");
        }
    }

    /**
     * Password cannot be null
     */
    public static void invariantPasswordNotNull(User entity) {
        if (entity.getPassword() == null) {
            throw new IllegalStateException("Password cannot be null");
        }
    }

    /**
     * Password cannot be blank
     */
    public static void invariantPasswordNotBlank(User entity) {
        if (entity.getPassword() == null || entity.getPassword().trim().isEmpty()) {
            throw new IllegalStateException("Password cannot be blank");
        }
    }

    /**
     * RealName cannot be null
     */
    public static void invariantRealNameNotNull(User entity) {
        if (entity.getRealName() == null) {
            throw new IllegalStateException("RealName cannot be null");
        }
    }

    /**
     * RealName cannot be blank
     */
    public static void invariantRealNameNotBlank(User entity) {
        if (entity.getRealName() == null || entity.getRealName().trim().isEmpty()) {
            throw new IllegalStateException("RealName cannot be blank");
        }
    }

    /**
     * Email cannot be null
     */
    public static void invariantEmailNotNull(User entity) {
        if (entity.getEmail() == null) {
            throw new IllegalStateException("Email cannot be null");
        }
    }

    /**
     * Email cannot be blank
     */
    public static void invariantEmailNotBlank(User entity) {
        if (entity.getEmail() == null || entity.getEmail().trim().isEmpty()) {
            throw new IllegalStateException("Email cannot be blank");
        }
    }

    /**
     * Email must be a valid email format
     */
    public static void invariantEmailEmailFormat(User entity) {
        if (entity.getEmail() != null && !entity.getEmail().matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new IllegalStateException("Email must be a valid email format");
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