package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.Order;

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
     * Time cannot be null
     */
    public static void invariantTimeNotNull(Order entity) {
        if (entity.getTime() == null) {
            throw new IllegalStateException("Time cannot be null");
        }
    }

    /**
     * Time cannot be blank
     */
    public static void invariantTimeNotBlank(Order entity) {
        if (entity.getTime() == null || entity.getTime().trim().isEmpty()) {
            throw new IllegalStateException("Time cannot be blank");
        }
    }

    /**
     * TotalPriceInCents cannot be null
     */
    public static void invariantTotalPriceInCentsNotNull(Order entity) {
        if (entity.getTotalPriceInCents() == null) {
            throw new IllegalStateException("TotalPriceInCents cannot be null");
        }
    }

    /**
     * AddressName cannot be null
     */
    public static void invariantAddressNameNotNull(Order entity) {
        if (entity.getAddressName() == null) {
            throw new IllegalStateException("AddressName cannot be null");
        }
    }

    /**
     * AddressName cannot be blank
     */
    public static void invariantAddressNameNotBlank(Order entity) {
        if (entity.getAddressName() == null || entity.getAddressName().trim().isEmpty()) {
            throw new IllegalStateException("AddressName cannot be blank");
        }
    }

    /**
     * Address1 cannot be null
     */
    public static void invariantAddress1NotNull(Order entity) {
        if (entity.getAddress1() == null) {
            throw new IllegalStateException("Address1 cannot be null");
        }
    }

    /**
     * Address1 cannot be blank
     */
    public static void invariantAddress1NotBlank(Order entity) {
        if (entity.getAddress1() == null || entity.getAddress1().trim().isEmpty()) {
            throw new IllegalStateException("Address1 cannot be blank");
        }
    }

    /**
     * Address2 cannot be null
     */
    public static void invariantAddress2NotNull(Order entity) {
        if (entity.getAddress2() == null) {
            throw new IllegalStateException("Address2 cannot be null");
        }
    }

    /**
     * Address2 cannot be blank
     */
    public static void invariantAddress2NotBlank(Order entity) {
        if (entity.getAddress2() == null || entity.getAddress2().trim().isEmpty()) {
            throw new IllegalStateException("Address2 cannot be blank");
        }
    }

    /**
     * CreditCardCompany cannot be null
     */
    public static void invariantCreditCardCompanyNotNull(Order entity) {
        if (entity.getCreditCardCompany() == null) {
            throw new IllegalStateException("CreditCardCompany cannot be null");
        }
    }

    /**
     * CreditCardCompany cannot be blank
     */
    public static void invariantCreditCardCompanyNotBlank(Order entity) {
        if (entity.getCreditCardCompany() == null || entity.getCreditCardCompany().trim().isEmpty()) {
            throw new IllegalStateException("CreditCardCompany cannot be blank");
        }
    }

    /**
     * CreditCardNumber cannot be null
     */
    public static void invariantCreditCardNumberNotNull(Order entity) {
        if (entity.getCreditCardNumber() == null) {
            throw new IllegalStateException("CreditCardNumber cannot be null");
        }
    }

    /**
     * CreditCardNumber cannot be blank
     */
    public static void invariantCreditCardNumberNotBlank(Order entity) {
        if (entity.getCreditCardNumber() == null || entity.getCreditCardNumber().trim().isEmpty()) {
            throw new IllegalStateException("CreditCardNumber cannot be blank");
        }
    }

    /**
     * CreditCardExpiryDate cannot be null
     */
    public static void invariantCreditCardExpiryDateNotNull(Order entity) {
        if (entity.getCreditCardExpiryDate() == null) {
            throw new IllegalStateException("CreditCardExpiryDate cannot be null");
        }
    }

    /**
     * CreditCardExpiryDate cannot be blank
     */
    public static void invariantCreditCardExpiryDateNotBlank(Order entity) {
        if (entity.getCreditCardExpiryDate() == null || entity.getCreditCardExpiryDate().trim().isEmpty()) {
            throw new IllegalStateException("CreditCardExpiryDate cannot be blank");
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