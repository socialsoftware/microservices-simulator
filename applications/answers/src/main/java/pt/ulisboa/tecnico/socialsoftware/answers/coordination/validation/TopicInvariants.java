package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;

/**
 * Invariant validation methods for Topic
 */
public class TopicInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(Topic entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(Topic entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * Course cannot be null
     */
    public static void invariantCourseNotNull(Topic entity) {
        if (entity.getCourse() == null) {
            throw new IllegalStateException("Course cannot be null");
        }
    }

    /**
     * Topic aggregate must be in a valid state
     */
    public static void invariantTopicValid(Topic entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}