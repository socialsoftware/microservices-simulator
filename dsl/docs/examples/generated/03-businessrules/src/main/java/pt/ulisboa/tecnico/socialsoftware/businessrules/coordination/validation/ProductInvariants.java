package pt.ulisboa.tecnico.socialsoftware.businessrules.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.aggregate.Product;

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
     * Sku cannot be null
     */
    public static void invariantSkuNotNull(Product entity) {
        if (entity.getSku() == null) {
            throw new IllegalStateException("Sku cannot be null");
        }
    }

    /**
     * Sku cannot be blank
     */
    public static void invariantSkuNotBlank(Product entity) {
        if (entity.getSku() == null || entity.getSku().trim().isEmpty()) {
            throw new IllegalStateException("Sku cannot be blank");
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
     * StockQuantity cannot be null
     */
    public static void invariantStockQuantityNotNull(Product entity) {
        if (entity.getStockQuantity() == null) {
            throw new IllegalStateException("StockQuantity cannot be null");
        }
    }

    /**
     * Active cannot be null
     */
    public static void invariantActiveNotNull(Product entity) {
        if (entity.getActive() == null) {
            throw new IllegalStateException("Active cannot be null");
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