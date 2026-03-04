package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.Course;

/**
 * Invariant validation methods for Course
 */
public class CourseInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(Course entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(Course entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * Type cannot be null
     */
    public static void invariantTypeNotNull(Course entity) {
        if (entity.getType() == null) {
            throw new IllegalStateException("Type cannot be null");
        }
    }

    /**
     * CreationDate cannot be null
     */
    public static void invariantCreationDateNotNull(Course entity) {
        if (entity.getCreationDate() == null) {
            throw new IllegalStateException("CreationDate cannot be null");
        }
    }

    /**
     * Course aggregate must be in a valid state
     */
    public static void invariantCourseValid(Course entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}