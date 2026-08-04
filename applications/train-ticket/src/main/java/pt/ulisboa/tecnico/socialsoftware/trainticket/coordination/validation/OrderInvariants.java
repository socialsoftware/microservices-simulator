package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;

/**
 * Invariant validation methods for Order
 */
public class OrderInvariants {

    /**
     * BoughtDate cannot be null
     */
    public static void invariantBoughtDateNotNull(Order entity) {
        if (entity.getBoughtDate() == null) {
            throw new IllegalStateException("BoughtDate cannot be null");
        }
    }

    /**
     * BoughtDate cannot be blank
     */
    public static void invariantBoughtDateNotBlank(Order entity) {
        if (entity.getBoughtDate() == null || entity.getBoughtDate().trim().isEmpty()) {
            throw new IllegalStateException("BoughtDate cannot be blank");
        }
    }

    /**
     * TravelDate cannot be null
     */
    public static void invariantTravelDateNotNull(Order entity) {
        if (entity.getTravelDate() == null) {
            throw new IllegalStateException("TravelDate cannot be null");
        }
    }

    /**
     * TravelDate cannot be blank
     */
    public static void invariantTravelDateNotBlank(Order entity) {
        if (entity.getTravelDate() == null || entity.getTravelDate().trim().isEmpty()) {
            throw new IllegalStateException("TravelDate cannot be blank");
        }
    }

    /**
     * TravelTime cannot be null
     */
    public static void invariantTravelTimeNotNull(Order entity) {
        if (entity.getTravelTime() == null) {
            throw new IllegalStateException("TravelTime cannot be null");
        }
    }

    /**
     * TravelTime cannot be blank
     */
    public static void invariantTravelTimeNotBlank(Order entity) {
        if (entity.getTravelTime() == null || entity.getTravelTime().trim().isEmpty()) {
            throw new IllegalStateException("TravelTime cannot be blank");
        }
    }

    /**
     * CoachNumber cannot be null
     */
    public static void invariantCoachNumberNotNull(Order entity) {
        if (entity.getCoachNumber() == null) {
            throw new IllegalStateException("CoachNumber cannot be null");
        }
    }

    /**
     * SeatClass cannot be null
     */
    public static void invariantSeatClassNotNull(Order entity) {
        if (entity.getSeatClass() == null) {
            throw new IllegalStateException("SeatClass cannot be null");
        }
    }

    /**
     * SeatNumber cannot be null
     */
    public static void invariantSeatNumberNotNull(Order entity) {
        if (entity.getSeatNumber() == null) {
            throw new IllegalStateException("SeatNumber cannot be null");
        }
    }

    /**
     * SeatNumber cannot be blank
     */
    public static void invariantSeatNumberNotBlank(Order entity) {
        if (entity.getSeatNumber() == null || entity.getSeatNumber().trim().isEmpty()) {
            throw new IllegalStateException("SeatNumber cannot be blank");
        }
    }

    /**
     * Price cannot be null
     */
    public static void invariantPriceNotNull(Order entity) {
        if (entity.getPrice() == null) {
            throw new IllegalStateException("Price cannot be null");
        }
    }

    /**
     * Status cannot be null
     */
    public static void invariantStatusNotNull(Order entity) {
        if (entity.getStatus() == null) {
            throw new IllegalStateException("Status cannot be null");
        }
    }

    /**
     * User cannot be null
     */
    public static void invariantUserNotNull(Order entity) {
        if (entity.getUser() == null) {
            throw new IllegalStateException("User cannot be null");
        }
    }

    /**
     * Contacts cannot be null
     */
    public static void invariantContactsNotNull(Order entity) {
        if (entity.getContacts() == null) {
            throw new IllegalStateException("Contacts cannot be null");
        }
    }

    /**
     * Trip cannot be null
     */
    public static void invariantTripNotNull(Order entity) {
        if (entity.getTrip() == null) {
            throw new IllegalStateException("Trip cannot be null");
        }
    }

    /**
     * Train cannot be null
     */
    public static void invariantTrainNotNull(Order entity) {
        if (entity.getTrain() == null) {
            throw new IllegalStateException("Train cannot be null");
        }
    }

    /**
     * FromStation cannot be null
     */
    public static void invariantFromStationNotNull(Order entity) {
        if (entity.getFromStation() == null) {
            throw new IllegalStateException("FromStation cannot be null");
        }
    }

    /**
     * ToStation cannot be null
     */
    public static void invariantToStationNotNull(Order entity) {
        if (entity.getToStation() == null) {
            throw new IllegalStateException("ToStation cannot be null");
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