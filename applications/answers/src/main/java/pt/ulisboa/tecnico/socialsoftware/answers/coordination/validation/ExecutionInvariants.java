package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;

/**
 * Invariant validation methods for Execution
 */
public class ExecutionInvariants {

    /**
     * Acronym cannot be null
     */
    public static void invariantAcronymNotNull(Execution entity) {
        if (entity.getAcronym() == null) {
            throw new IllegalStateException("Acronym cannot be null");
        }
    }

    /**
     * Acronym cannot be blank
     */
    public static void invariantAcronymNotBlank(Execution entity) {
        if (entity.getAcronym() == null || entity.getAcronym().trim().isEmpty()) {
            throw new IllegalStateException("Acronym cannot be blank");
        }
    }

    /**
     * AcademicTerm cannot be null
     */
    public static void invariantAcademicTermNotNull(Execution entity) {
        if (entity.getAcademicTerm() == null) {
            throw new IllegalStateException("AcademicTerm cannot be null");
        }
    }

    /**
     * AcademicTerm cannot be blank
     */
    public static void invariantAcademicTermNotBlank(Execution entity) {
        if (entity.getAcademicTerm() == null || entity.getAcademicTerm().trim().isEmpty()) {
            throw new IllegalStateException("AcademicTerm cannot be blank");
        }
    }

    /**
     * EndDate cannot be null
     */
    public static void invariantEndDateNotNull(Execution entity) {
        if (entity.getEndDate() == null) {
            throw new IllegalStateException("EndDate cannot be null");
        }
    }

    /**
     * Course cannot be null
     */
    public static void invariantCourseNotNull(Execution entity) {
        if (entity.getCourse() == null) {
            throw new IllegalStateException("Course cannot be null");
        }
    }

    /**
     * Users cannot be null
     */
    public static void invariantUsersNotNull(Execution entity) {
        if (entity.getUsers() == null) {
            throw new IllegalStateException("Users cannot be null");
        }
    }

    /**
     * Users cannot be empty
     */
    public static void invariantUsersNotEmpty(Execution entity) {
        if (entity.getUsers() == null || ((java.util.Collection) entity.getUsers()).isEmpty()) {
            throw new IllegalStateException("Users cannot be empty");
        }
    }

    /**
     * Execution aggregate must be in a valid state
     */
    public static void invariantExecutionValid(Execution entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}