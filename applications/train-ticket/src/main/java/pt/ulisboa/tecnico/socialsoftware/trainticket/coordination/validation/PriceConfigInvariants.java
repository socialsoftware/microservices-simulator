package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;

/**
 * Invariant validation methods for PriceConfig
 */
public class PriceConfigInvariants {

    /**
     * BasicPriceRate cannot be null
     */
    public static void invariantBasicPriceRateNotNull(PriceConfig entity) {
        if (entity.getBasicPriceRate() == null) {
            throw new IllegalStateException("BasicPriceRate cannot be null");
        }
    }

    /**
     * FirstClassPriceRate cannot be null
     */
    public static void invariantFirstClassPriceRateNotNull(PriceConfig entity) {
        if (entity.getFirstClassPriceRate() == null) {
            throw new IllegalStateException("FirstClassPriceRate cannot be null");
        }
    }

    /**
     * TrainType cannot be null
     */
    public static void invariantTrainTypeNotNull(PriceConfig entity) {
        if (entity.getTrainType() == null) {
            throw new IllegalStateException("TrainType cannot be null");
        }
    }

    /**
     * Route cannot be null
     */
    public static void invariantRouteNotNull(PriceConfig entity) {
        if (entity.getRoute() == null) {
            throw new IllegalStateException("Route cannot be null");
        }
    }

    /**
     * PriceConfig aggregate must be in a valid state
     */
    public static void invariantPriceConfigValid(PriceConfig entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}