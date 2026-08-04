package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station;

/**
 * Invariant validation methods for Station
 */
public class StationInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(Station entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(Station entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * StayTime cannot be null
     */
    public static void invariantStayTimeNotNull(Station entity) {
        if (entity.getStayTime() == null) {
            throw new IllegalStateException("StayTime cannot be null");
        }
    }

    /**
     * Station aggregate must be in a valid state
     */
    public static void invariantStationValid(Station entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}