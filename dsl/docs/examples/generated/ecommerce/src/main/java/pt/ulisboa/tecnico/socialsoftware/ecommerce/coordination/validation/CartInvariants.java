package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.Cart;

/**
 * Invariant validation methods for Cart
 */
public class CartInvariants {

    /**
     * User cannot be null
     */
    public static void invariantUserNotNull(Cart entity) {
        if (entity.getUser() == null) {
            throw new IllegalStateException("User cannot be null");
        }
    }

    /**
     * TotalInCents cannot be null
     */
    public static void invariantTotalInCentsNotNull(Cart entity) {
        if (entity.getTotalInCents() == null) {
            throw new IllegalStateException("TotalInCents cannot be null");
        }
    }

    /**
     * ItemCount cannot be null
     */
    public static void invariantItemCountNotNull(Cart entity) {
        if (entity.getItemCount() == null) {
            throw new IllegalStateException("ItemCount cannot be null");
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
     * Cart aggregate must be in a valid state
     */
    public static void invariantCartValid(Cart entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}