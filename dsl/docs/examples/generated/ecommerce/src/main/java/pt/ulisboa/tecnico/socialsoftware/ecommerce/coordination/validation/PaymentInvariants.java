package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;

/**
 * Invariant validation methods for Payment
 */
public class PaymentInvariants {

    /**
     * Order cannot be null
     */
    public static void invariantOrderNotNull(Payment entity) {
        if (entity.getOrder() == null) {
            throw new IllegalStateException("Order cannot be null");
        }
    }

    /**
     * AmountInCents cannot be null
     */
    public static void invariantAmountInCentsNotNull(Payment entity) {
        if (entity.getAmountInCents() == null) {
            throw new IllegalStateException("AmountInCents cannot be null");
        }
    }

    /**
     * Status cannot be null
     */
    public static void invariantStatusNotNull(Payment entity) {
        if (entity.getStatus() == null) {
            throw new IllegalStateException("Status cannot be null");
        }
    }

    /**
     * AuthorizationCode cannot be null
     */
    public static void invariantAuthorizationCodeNotNull(Payment entity) {
        if (entity.getAuthorizationCode() == null) {
            throw new IllegalStateException("AuthorizationCode cannot be null");
        }
    }

    /**
     * AuthorizationCode cannot be blank
     */
    public static void invariantAuthorizationCodeNotBlank(Payment entity) {
        if (entity.getAuthorizationCode() == null || entity.getAuthorizationCode().trim().isEmpty()) {
            throw new IllegalStateException("AuthorizationCode cannot be blank");
        }
    }

    /**
     * PaymentMethod cannot be null
     */
    public static void invariantPaymentMethodNotNull(Payment entity) {
        if (entity.getPaymentMethod() == null) {
            throw new IllegalStateException("PaymentMethod cannot be null");
        }
    }

    /**
     * PaymentMethod cannot be blank
     */
    public static void invariantPaymentMethodNotBlank(Payment entity) {
        if (entity.getPaymentMethod() == null || entity.getPaymentMethod().trim().isEmpty()) {
            throw new IllegalStateException("PaymentMethod cannot be blank");
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