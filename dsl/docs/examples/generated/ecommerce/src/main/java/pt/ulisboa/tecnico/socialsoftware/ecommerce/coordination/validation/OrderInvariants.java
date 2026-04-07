package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;

/**
 * Invariant validation methods for Order
 */
public class OrderInvariants {

    /**
     * User cannot be null
     */
    public static void invariantUserNotNull(Order entity) {
        if (entity.getUser() == null) {
            throw new IllegalStateException("User cannot be null");
        }
    }

    /**
     * TotalInCents cannot be null
     */
    public static void invariantTotalInCentsNotNull(Order entity) {
        if (entity.getTotalInCents() == null) {
            throw new IllegalStateException("TotalInCents cannot be null");
        }
    }

    /**
     * ItemCount cannot be null
     */
    public static void invariantItemCountNotNull(Order entity) {
        if (entity.getItemCount() == null) {
            throw new IllegalStateException("ItemCount cannot be null");
        }
    }

    /**
     * Status cannot be null
     */
    public static void invariantStatusNotNull(Order entity) {
        if (entity.getStatus() == null) {
            throw new IllegalStateException("Status cannot be null");
        }
    }

    /**
     * PlacedAt cannot be null
     */
    public static void invariantPlacedAtNotNull(Order entity) {
        if (entity.getPlacedAt() == null) {
            throw new IllegalStateException("PlacedAt cannot be null");
        }
    }

    /**
     * PlacedAt cannot be blank
     */
    public static void invariantPlacedAtNotBlank(Order entity) {
        if (entity.getPlacedAt() == null || entity.getPlacedAt().trim().isEmpty()) {
            throw new IllegalStateException("PlacedAt cannot be blank");
        }
    }

    /**
     * Order aggregate must be in a valid state
     */
    public static void invariantOrderValid(Order entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}