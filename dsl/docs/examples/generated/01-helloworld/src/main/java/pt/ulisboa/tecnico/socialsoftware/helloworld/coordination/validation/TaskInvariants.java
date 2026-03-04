package pt.ulisboa.tecnico.socialsoftware.helloworld.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.Task;

/**
 * Invariant validation methods for Task
 */
public class TaskInvariants {

    /**
     * Title cannot be null
     */
    public static void invariantTitleNotNull(Task entity) {
        if (entity.getTitle() == null) {
            throw new IllegalStateException("Title cannot be null");
        }
    }

    /**
     * Title cannot be blank
     */
    public static void invariantTitleNotBlank(Task entity) {
        if (entity.getTitle() == null || entity.getTitle().trim().isEmpty()) {
            throw new IllegalStateException("Title cannot be blank");
        }
    }

    /**
     * Description cannot be null
     */
    public static void invariantDescriptionNotNull(Task entity) {
        if (entity.getDescription() == null) {
            throw new IllegalStateException("Description cannot be null");
        }
    }

    /**
     * Description cannot be blank
     */
    public static void invariantDescriptionNotBlank(Task entity) {
        if (entity.getDescription() == null || entity.getDescription().trim().isEmpty()) {
            throw new IllegalStateException("Description cannot be blank");
        }
    }

    /**
     * Done cannot be null
     */
    public static void invariantDoneNotNull(Task entity) {
        if (entity.getDone() == null) {
            throw new IllegalStateException("Done cannot be null");
        }
    }

    /**
     * Task aggregate must be in a valid state
     */
    public static void invariantTaskValid(Task entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}