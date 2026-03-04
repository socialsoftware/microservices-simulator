package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.aggregate.Product;

/**
 * Invariant validation methods for Product
 */
public class ProductInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(Product entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(Product entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * Price cannot be null
     */
    public static void invariantPriceNotNull(Product entity) {
        if (entity.getPrice() == null) {
            throw new IllegalStateException("Price cannot be null");
        }
    }

    /**
     * Available cannot be null
     */
    public static void invariantAvailableNotNull(Product entity) {
        if (entity.getAvailable() == null) {
            throw new IllegalStateException("Available cannot be null");
        }
    }

    /**
     * Product aggregate must be in a valid state
     */
    public static void invariantProductValid(Product entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}