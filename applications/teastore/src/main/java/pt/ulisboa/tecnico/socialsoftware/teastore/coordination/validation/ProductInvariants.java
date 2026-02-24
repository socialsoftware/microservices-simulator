package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;

/**
 * Invariant validation methods for Product
 */
public class ProductInvariants {

    /**
     * ProductCategory cannot be null
     */
    public static void invariantProductCategoryNotNull(Product entity) {
        if (entity.getProductCategory() == null) {
            throw new IllegalStateException("ProductCategory cannot be null");
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
     * ListPriceInCents cannot be null
     */
    public static void invariantListPriceInCentsNotNull(Product entity) {
        if (entity.getListPriceInCents() == null) {
            throw new IllegalStateException("ListPriceInCents cannot be null");
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