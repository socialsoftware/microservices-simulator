package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;

/**
 * Invariant validation methods for Shipping
 */
public class ShippingInvariants {

    /**
     * Order cannot be null
     */
    public static void invariantOrderNotNull(Shipping entity) {
        if (entity.getOrder() == null) {
            throw new IllegalStateException("Order cannot be null");
        }
    }

    /**
     * Address cannot be null
     */
    public static void invariantAddressNotNull(Shipping entity) {
        if (entity.getAddress() == null) {
            throw new IllegalStateException("Address cannot be null");
        }
    }

    /**
     * Address cannot be blank
     */
    public static void invariantAddressNotBlank(Shipping entity) {
        if (entity.getAddress() == null || entity.getAddress().trim().isEmpty()) {
            throw new IllegalStateException("Address cannot be blank");
        }
    }

    /**
     * Carrier cannot be null
     */
    public static void invariantCarrierNotNull(Shipping entity) {
        if (entity.getCarrier() == null) {
            throw new IllegalStateException("Carrier cannot be null");
        }
    }

    /**
     * Carrier cannot be blank
     */
    public static void invariantCarrierNotBlank(Shipping entity) {
        if (entity.getCarrier() == null || entity.getCarrier().trim().isEmpty()) {
            throw new IllegalStateException("Carrier cannot be blank");
        }
    }

    /**
     * TrackingNumber cannot be null
     */
    public static void invariantTrackingNumberNotNull(Shipping entity) {
        if (entity.getTrackingNumber() == null) {
            throw new IllegalStateException("TrackingNumber cannot be null");
        }
    }

    /**
     * TrackingNumber cannot be blank
     */
    public static void invariantTrackingNumberNotBlank(Shipping entity) {
        if (entity.getTrackingNumber() == null || entity.getTrackingNumber().trim().isEmpty()) {
            throw new IllegalStateException("TrackingNumber cannot be blank");
        }
    }

    /**
     * Status cannot be null
     */
    public static void invariantStatusNotNull(Shipping entity) {
        if (entity.getStatus() == null) {
            throw new IllegalStateException("Status cannot be null");
        }
    }

    /**
     * Shipping aggregate must be in a valid state
     */
    public static void invariantShippingValid(Shipping entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}