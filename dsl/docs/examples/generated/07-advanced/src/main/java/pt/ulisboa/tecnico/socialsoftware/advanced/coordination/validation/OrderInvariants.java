package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.Order;

/**
 * Invariant validation methods for Order
 */
public class OrderInvariants {

    /**
     * Customer cannot be null
     */
    public static void invariantCustomerNotNull(Order entity) {
        if (entity.getCustomer() == null) {
            throw new IllegalStateException("Customer cannot be null");
        }
    }

    /**
     * Products cannot be null
     */
    public static void invariantProductsNotNull(Order entity) {
        if (entity.getProducts() == null) {
            throw new IllegalStateException("Products cannot be null");
        }
    }

    /**
     * Products cannot be empty
     */
    public static void invariantProductsNotEmpty(Order entity) {
        if (entity.getProducts() == null || ((java.util.Collection) entity.getProducts()).isEmpty()) {
            throw new IllegalStateException("Products cannot be empty");
        }
    }

    /**
     * Items cannot be null
     */
    public static void invariantItemsNotNull(Order entity) {
        if (entity.getItems() == null) {
            throw new IllegalStateException("Items cannot be null");
        }
    }

    /**
     * Items cannot be empty
     */
    public static void invariantItemsNotEmpty(Order entity) {
        if (entity.getItems() == null || ((java.util.Collection) entity.getItems()).isEmpty()) {
            throw new IllegalStateException("Items cannot be empty");
        }
    }

    /**
     * TotalAmount cannot be null
     */
    public static void invariantTotalAmountNotNull(Order entity) {
        if (entity.getTotalAmount() == null) {
            throw new IllegalStateException("TotalAmount cannot be null");
        }
    }

    /**
     * OrderDate cannot be null
     */
    public static void invariantOrderDateNotNull(Order entity) {
        if (entity.getOrderDate() == null) {
            throw new IllegalStateException("OrderDate cannot be null");
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
     * PaymentMethod cannot be null
     */
    public static void invariantPaymentMethodNotNull(Order entity) {
        if (entity.getPaymentMethod() == null) {
            throw new IllegalStateException("PaymentMethod cannot be null");
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