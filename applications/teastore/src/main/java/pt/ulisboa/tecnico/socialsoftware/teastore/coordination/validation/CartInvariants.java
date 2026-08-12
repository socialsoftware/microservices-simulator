package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

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