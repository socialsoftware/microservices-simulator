package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.Invoice;

/**
 * Invariant validation methods for Invoice
 */
public class InvoiceInvariants {

    /**
     * Order cannot be null
     */
    public static void invariantOrderNotNull(Invoice entity) {
        if (entity.getOrder() == null) {
            throw new IllegalStateException("Order cannot be null");
        }
    }

    /**
     * User cannot be null
     */
    public static void invariantUserNotNull(Invoice entity) {
        if (entity.getUser() == null) {
            throw new IllegalStateException("User cannot be null");
        }
    }

    /**
     * InvoiceNumber cannot be null
     */
    public static void invariantInvoiceNumberNotNull(Invoice entity) {
        if (entity.getInvoiceNumber() == null) {
            throw new IllegalStateException("InvoiceNumber cannot be null");
        }
    }

    /**
     * InvoiceNumber cannot be blank
     */
    public static void invariantInvoiceNumberNotBlank(Invoice entity) {
        if (entity.getInvoiceNumber() == null || entity.getInvoiceNumber().trim().isEmpty()) {
            throw new IllegalStateException("InvoiceNumber cannot be blank");
        }
    }

    /**
     * AmountInCents cannot be null
     */
    public static void invariantAmountInCentsNotNull(Invoice entity) {
        if (entity.getAmountInCents() == null) {
            throw new IllegalStateException("AmountInCents cannot be null");
        }
    }

    /**
     * IssuedAt cannot be null
     */
    public static void invariantIssuedAtNotNull(Invoice entity) {
        if (entity.getIssuedAt() == null) {
            throw new IllegalStateException("IssuedAt cannot be null");
        }
    }

    /**
     * IssuedAt cannot be blank
     */
    public static void invariantIssuedAtNotBlank(Invoice entity) {
        if (entity.getIssuedAt() == null || entity.getIssuedAt().trim().isEmpty()) {
            throw new IllegalStateException("IssuedAt cannot be blank");
        }
    }

    /**
     * Status cannot be null
     */
    public static void invariantStatusNotNull(Invoice entity) {
        if (entity.getStatus() == null) {
            throw new IllegalStateException("Status cannot be null");
        }
    }

    /**
     * Invoice aggregate must be in a valid state
     */
    public static void invariantInvoiceValid(Invoice entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}