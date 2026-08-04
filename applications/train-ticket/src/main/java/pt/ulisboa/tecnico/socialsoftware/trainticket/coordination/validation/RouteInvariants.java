package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.Route;

/**
 * Invariant validation methods for Route
 */
public class RouteInvariants {

    /**
     * Stations cannot be null
     */
    public static void invariantStationsNotNull(Route entity) {
        if (entity.getStations() == null) {
            throw new IllegalStateException("Stations cannot be null");
        }
    }

    /**
     * Stations cannot be empty
     */
    public static void invariantStationsNotEmpty(Route entity) {
        if (entity.getStations() == null || ((java.util.Collection) entity.getStations()).isEmpty()) {
            throw new IllegalStateException("Stations cannot be empty");
        }
    }

    /**
     * Route aggregate must be in a valid state
     */
    public static void invariantRouteValid(Route entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}