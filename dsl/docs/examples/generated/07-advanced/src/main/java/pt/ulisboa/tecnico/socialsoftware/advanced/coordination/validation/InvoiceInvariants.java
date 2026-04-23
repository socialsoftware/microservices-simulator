package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.Invoice;

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
     * Customer cannot be null
     */
    public static void invariantCustomerNotNull(Invoice entity) {
        if (entity.getCustomer() == null) {
            throw new IllegalStateException("Customer cannot be null");
        }
    }

    /**
     * TotalAmount cannot be null
     */
    public static void invariantTotalAmountNotNull(Invoice entity) {
        if (entity.getTotalAmount() == null) {
            throw new IllegalStateException("TotalAmount cannot be null");
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
     * Paid cannot be null
     */
    public static void invariantPaidNotNull(Invoice entity) {
        if (entity.getPaid() == null) {
            throw new IllegalStateException("Paid cannot be null");
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