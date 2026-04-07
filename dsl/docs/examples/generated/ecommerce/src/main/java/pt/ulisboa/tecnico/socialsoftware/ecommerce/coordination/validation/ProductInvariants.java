package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.product.aggregate.Product;

/**
 * Invariant validation methods for Product
 */
public class ProductInvariants {

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
     * Description cannot be null
     */
    public static void invariantDescriptionNotNull(Product entity) {
        if (entity.getDescription() == null) {
            throw new IllegalStateException("Description cannot be null");
        }
    }

    /**
     * Description cannot be blank
     */
    public static void invariantDescriptionNotBlank(Product entity) {
        if (entity.getDescription() == null || entity.getDescription().trim().isEmpty()) {
            throw new IllegalStateException("Description cannot be blank");
        }
    }

    /**
     * PriceInCents cannot be null
     */
    public static void invariantPriceInCentsNotNull(Product entity) {
        if (entity.getPriceInCents() == null) {
            throw new IllegalStateException("PriceInCents cannot be null");
        }
    }

    /**
     * Stock cannot be null
     */
    public static void invariantStockNotNull(Product entity) {
        if (entity.getStock() == null) {
            throw new IllegalStateException("Stock cannot be null");
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