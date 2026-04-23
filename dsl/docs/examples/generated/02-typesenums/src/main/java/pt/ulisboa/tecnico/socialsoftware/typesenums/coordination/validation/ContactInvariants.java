package pt.ulisboa.tecnico.socialsoftware.typesenums.coordination.validation;

import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.Contact;

/**
 * Invariant validation methods for Contact
 */
public class ContactInvariants {

    /**
     * FirstName cannot be null
     */
    public static void invariantFirstNameNotNull(Contact entity) {
        if (entity.getFirstName() == null) {
            throw new IllegalStateException("FirstName cannot be null");
        }
    }

    /**
     * FirstName cannot be blank
     */
    public static void invariantFirstNameNotBlank(Contact entity) {
        if (entity.getFirstName() == null || entity.getFirstName().trim().isEmpty()) {
            throw new IllegalStateException("FirstName cannot be blank");
        }
    }

    /**
     * LastName cannot be null
     */
    public static void invariantLastNameNotNull(Contact entity) {
        if (entity.getLastName() == null) {
            throw new IllegalStateException("LastName cannot be null");
        }
    }

    /**
     * LastName cannot be blank
     */
    public static void invariantLastNameNotBlank(Contact entity) {
        if (entity.getLastName() == null || entity.getLastName().trim().isEmpty()) {
            throw new IllegalStateException("LastName cannot be blank");
        }
    }

    /**
     * Email cannot be null
     */
    public static void invariantEmailNotNull(Contact entity) {
        if (entity.getEmail() == null) {
            throw new IllegalStateException("Email cannot be null");
        }
    }

    /**
     * Email cannot be blank
     */
    public static void invariantEmailNotBlank(Contact entity) {
        if (entity.getEmail() == null || entity.getEmail().trim().isEmpty()) {
            throw new IllegalStateException("Email cannot be blank");
        }
    }

    /**
     * Email must be a valid email format
     */
    public static void invariantEmailEmailFormat(Contact entity) {
        if (entity.getEmail() != null && !entity.getEmail().matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new IllegalStateException("Email must be a valid email format");
        }
    }

    /**
     * Category cannot be null
     */
    public static void invariantCategoryNotNull(Contact entity) {
        if (entity.getCategory() == null) {
            throw new IllegalStateException("Category cannot be null");
        }
    }

    /**
     * CreatedAt cannot be null
     */
    public static void invariantCreatedAtNotNull(Contact entity) {
        if (entity.getCreatedAt() == null) {
            throw new IllegalStateException("CreatedAt cannot be null");
        }
    }

    /**
     * CreatedAt must be in the past
     */
    public static void invariantCreatedAtPast(Contact entity) {
        if (entity.getCreatedAt() != null && !entity.getCreatedAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("CreatedAt must be in the past");
        }
    }

    /**
     * Favorite cannot be null
     */
    public static void invariantFavoriteNotNull(Contact entity) {
        if (entity.getFavorite() == null) {
            throw new IllegalStateException("Favorite cannot be null");
        }
    }

    /**
     * CallCount cannot be null
     */
    public static void invariantCallCountNotNull(Contact entity) {
        if (entity.getCallCount() == null) {
            throw new IllegalStateException("CallCount cannot be null");
        }
    }

    /**
     * Contact aggregate must be in a valid state
     */
    public static void invariantContactValid(Contact entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}