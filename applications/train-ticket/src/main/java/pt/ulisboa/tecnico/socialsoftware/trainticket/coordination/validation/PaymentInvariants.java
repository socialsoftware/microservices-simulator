package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.Payment;

/**
 * Invariant validation methods for Payment
 */
public class PaymentInvariants {

    /**
     * Amount cannot be null
     */
    public static void invariantAmountNotNull(Payment entity) {
        if (entity.getAmount() == null) {
            throw new IllegalStateException("Amount cannot be null");
        }
    }

    /**
     * Type cannot be null
     */
    public static void invariantTypeNotNull(Payment entity) {
        if (entity.getType() == null) {
            throw new IllegalStateException("Type cannot be null");
        }
    }

    /**
     * PaymentDate cannot be null
     */
    public static void invariantPaymentDateNotNull(Payment entity) {
        if (entity.getPaymentDate() == null) {
            throw new IllegalStateException("PaymentDate cannot be null");
        }
    }

    /**
     * PaymentDate cannot be blank
     */
    public static void invariantPaymentDateNotBlank(Payment entity) {
        if (entity.getPaymentDate() == null || entity.getPaymentDate().trim().isEmpty()) {
            throw new IllegalStateException("PaymentDate cannot be blank");
        }
    }

    /**
     * Order cannot be null
     */
    public static void invariantOrderNotNull(Payment entity) {
        if (entity.getOrder() == null) {
            throw new IllegalStateException("Order cannot be null");
        }
    }

    /**
     * User cannot be null
     */
    public static void invariantUserNotNull(Payment entity) {
        if (entity.getUser() == null) {
            throw new IllegalStateException("User cannot be null");
        }
    }

    /**
     * Payment aggregate must be in a valid state
     */
    public static void invariantPaymentValid(Payment entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}