package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.User;

/**
 * Invariant validation methods for User
 */
public class UserInvariants {

    /**
     * UserName cannot be null
     */
    public static void invariantUserNameNotNull(User entity) {
        if (entity.getUserName() == null) {
            throw new IllegalStateException("UserName cannot be null");
        }
    }

    /**
     * UserName cannot be blank
     */
    public static void invariantUserNameNotBlank(User entity) {
        if (entity.getUserName() == null || entity.getUserName().trim().isEmpty()) {
            throw new IllegalStateException("UserName cannot be blank");
        }
    }

    /**
     * Password cannot be null
     */
    public static void invariantPasswordNotNull(User entity) {
        if (entity.getPassword() == null) {
            throw new IllegalStateException("Password cannot be null");
        }
    }

    /**
     * Password cannot be blank
     */
    public static void invariantPasswordNotBlank(User entity) {
        if (entity.getPassword() == null || entity.getPassword().trim().isEmpty()) {
            throw new IllegalStateException("Password cannot be blank");
        }
    }

    /**
     * Gender cannot be null
     */
    public static void invariantGenderNotNull(User entity) {
        if (entity.getGender() == null) {
            throw new IllegalStateException("Gender cannot be null");
        }
    }

    /**
     * DocumentType cannot be null
     */
    public static void invariantDocumentTypeNotNull(User entity) {
        if (entity.getDocumentType() == null) {
            throw new IllegalStateException("DocumentType cannot be null");
        }
    }

    /**
     * DocumentNum cannot be null
     */
    public static void invariantDocumentNumNotNull(User entity) {
        if (entity.getDocumentNum() == null) {
            throw new IllegalStateException("DocumentNum cannot be null");
        }
    }

    /**
     * DocumentNum cannot be blank
     */
    public static void invariantDocumentNumNotBlank(User entity) {
        if (entity.getDocumentNum() == null || entity.getDocumentNum().trim().isEmpty()) {
            throw new IllegalStateException("DocumentNum cannot be blank");
        }
    }

    /**
     * Email cannot be null
     */
    public static void invariantEmailNotNull(User entity) {
        if (entity.getEmail() == null) {
            throw new IllegalStateException("Email cannot be null");
        }
    }

    /**
     * Email cannot be blank
     */
    public static void invariantEmailNotBlank(User entity) {
        if (entity.getEmail() == null || entity.getEmail().trim().isEmpty()) {
            throw new IllegalStateException("Email cannot be blank");
        }
    }

    /**
     * Email must be a valid email format
     */
    public static void invariantEmailEmailFormat(User entity) {
        if (entity.getEmail() != null && !entity.getEmail().matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new IllegalStateException("Email must be a valid email format");
        }
    }

    /**
     * Balance cannot be null
     */
    public static void invariantBalanceNotNull(User entity) {
        if (entity.getBalance() == null) {
            throw new IllegalStateException("Balance cannot be null");
        }
    }

    /**
     * User aggregate must be in a valid state
     */
    public static void invariantUserValid(User entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}