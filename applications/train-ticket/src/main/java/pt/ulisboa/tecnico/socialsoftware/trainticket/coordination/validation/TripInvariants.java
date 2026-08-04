package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;

/**
 * Invariant validation methods for Trip
 */
public class TripInvariants {

    /**
     * TripType cannot be null
     */
    public static void invariantTripTypeNotNull(Trip entity) {
        if (entity.getTripType() == null) {
            throw new IllegalStateException("TripType cannot be null");
        }
    }

    /**
     * TripNumber cannot be null
     */
    public static void invariantTripNumberNotNull(Trip entity) {
        if (entity.getTripNumber() == null) {
            throw new IllegalStateException("TripNumber cannot be null");
        }
    }

    /**
     * TripNumber cannot be blank
     */
    public static void invariantTripNumberNotBlank(Trip entity) {
        if (entity.getTripNumber() == null || entity.getTripNumber().trim().isEmpty()) {
            throw new IllegalStateException("TripNumber cannot be blank");
        }
    }

    /**
     * StartTime cannot be null
     */
    public static void invariantStartTimeNotNull(Trip entity) {
        if (entity.getStartTime() == null) {
            throw new IllegalStateException("StartTime cannot be null");
        }
    }

    /**
     * StartTime cannot be blank
     */
    public static void invariantStartTimeNotBlank(Trip entity) {
        if (entity.getStartTime() == null || entity.getStartTime().trim().isEmpty()) {
            throw new IllegalStateException("StartTime cannot be blank");
        }
    }

    /**
     * EndTime cannot be null
     */
    public static void invariantEndTimeNotNull(Trip entity) {
        if (entity.getEndTime() == null) {
            throw new IllegalStateException("EndTime cannot be null");
        }
    }

    /**
     * EndTime cannot be blank
     */
    public static void invariantEndTimeNotBlank(Trip entity) {
        if (entity.getEndTime() == null || entity.getEndTime().trim().isEmpty()) {
            throw new IllegalStateException("EndTime cannot be blank");
        }
    }

    /**
     * TrainType cannot be null
     */
    public static void invariantTrainTypeNotNull(Trip entity) {
        if (entity.getTrainType() == null) {
            throw new IllegalStateException("TrainType cannot be null");
        }
    }

    /**
     * Route cannot be null
     */
    public static void invariantRouteNotNull(Trip entity) {
        if (entity.getRoute() == null) {
            throw new IllegalStateException("Route cannot be null");
        }
    }

    /**
     * StartStation cannot be null
     */
    public static void invariantStartStationNotNull(Trip entity) {
        if (entity.getStartStation() == null) {
            throw new IllegalStateException("StartStation cannot be null");
        }
    }

    /**
     * TerminalStation cannot be null
     */
    public static void invariantTerminalStationNotNull(Trip entity) {
        if (entity.getTerminalStation() == null) {
            throw new IllegalStateException("TerminalStation cannot be null");
        }
    }

    /**
     * Trip aggregate must be in a valid state
     */
    public static void invariantTripValid(Trip entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}