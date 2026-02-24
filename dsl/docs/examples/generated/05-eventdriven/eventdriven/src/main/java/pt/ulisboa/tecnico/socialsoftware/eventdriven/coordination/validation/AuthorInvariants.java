package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.Author;

/**
 * Invariant validation methods for Author
 */
public class AuthorInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(Author entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(Author entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * Bio cannot be null
     */
    public static void invariantBioNotNull(Author entity) {
        if (entity.getBio() == null) {
            throw new IllegalStateException("Bio cannot be null");
        }
    }

    /**
     * Bio cannot be blank
     */
    public static void invariantBioNotBlank(Author entity) {
        if (entity.getBio() == null || entity.getBio().trim().isEmpty()) {
            throw new IllegalStateException("Bio cannot be blank");
        }
    }

    /**
     * Author aggregate must be in a valid state
     */
    public static void invariantAuthorValid(Author entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}