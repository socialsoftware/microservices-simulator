package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.Train;

/**
 * Invariant validation methods for Train
 */
public class TrainInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(Train entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(Train entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * EconomyClass cannot be null
     */
    public static void invariantEconomyClassNotNull(Train entity) {
        if (entity.getEconomyClass() == null) {
            throw new IllegalStateException("EconomyClass cannot be null");
        }
    }

    /**
     * ConfortClass cannot be null
     */
    public static void invariantConfortClassNotNull(Train entity) {
        if (entity.getConfortClass() == null) {
            throw new IllegalStateException("ConfortClass cannot be null");
        }
    }

    /**
     * AverageSpeed cannot be null
     */
    public static void invariantAverageSpeedNotNull(Train entity) {
        if (entity.getAverageSpeed() == null) {
            throw new IllegalStateException("AverageSpeed cannot be null");
        }
    }

    /**
     * Train aggregate must be in a valid state
     */
    public static void invariantTrainValid(Train entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}