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
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.Cart;

/**
 * Invariant validation methods for Cart
 */
public class CartInvariants {

    /**
     * UserId cannot be null
     */
    public static void invariantUserIdNotNull(Cart entity) {
        if (entity.getUserId() == null) {
            throw new IllegalStateException("UserId cannot be null");
        }
    }

    /**
     * CheckedOut cannot be null
     */
    public static void invariantCheckedOutNotNull(Cart entity) {
        if (entity.getCheckedOut() == null) {
            throw new IllegalStateException("CheckedOut cannot be null");
        }
    }

    /**
     * TotalPrice cannot be null
     */
    public static void invariantTotalPriceNotNull(Cart entity) {
        if (entity.getTotalPrice() == null) {
            throw new IllegalStateException("TotalPrice cannot be null");
        }
    }

    /**
     * Cart aggregate must be in a valid state
     */
    public static void invariantCartValid(Cart entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}