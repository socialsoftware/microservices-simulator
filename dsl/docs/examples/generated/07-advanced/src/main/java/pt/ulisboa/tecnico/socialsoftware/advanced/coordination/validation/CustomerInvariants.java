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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.regex.Pattern;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.Customer;

/**
 * Invariant validation methods for Customer
 */
public class CustomerInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(Customer entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(Customer entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * Email cannot be null
     */
    public static void invariantEmailNotNull(Customer entity) {
        if (entity.getEmail() == null) {
            throw new IllegalStateException("Email cannot be null");
        }
    }

    /**
     * Email cannot be blank
     */
    public static void invariantEmailNotBlank(Customer entity) {
        if (entity.getEmail() == null || entity.getEmail().trim().isEmpty()) {
            throw new IllegalStateException("Email cannot be blank");
        }
    }

    /**
     * Email must be a valid email format
     */
    public static void invariantEmailEmailFormat(Customer entity) {
        if (entity.getEmail() != null && !entity.getEmail().matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new IllegalStateException("Email must be a valid email format");
        }
    }

    /**
     * Active cannot be null
     */
    public static void invariantActiveNotNull(Customer entity) {
        if (entity.getActive() == null) {
            throw new IllegalStateException("Active cannot be null");
        }
    }

    /**
     * Customer aggregate must be in a valid state
     */
    public static void invariantCustomerValid(Customer entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}