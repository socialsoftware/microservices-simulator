package pt.ulisboa.tecnico.socialsoftware.ecommerce.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.aggregate.User;

/**
 * Invariant validation methods for User
 */
public class UserInvariants {

    /**
     * Username cannot be null
     */
    public static void invariantUsernameNotNull(User entity) {
        if (entity.getUsername() == null) {
            throw new IllegalStateException("Username cannot be null");
        }
    }

    /**
     * Username cannot be blank
     */
    public static void invariantUsernameNotBlank(User entity) {
        if (entity.getUsername() == null || entity.getUsername().trim().isEmpty()) {
            throw new IllegalStateException("Username cannot be blank");
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
     * PasswordHash cannot be null
     */
    public static void invariantPasswordHashNotNull(User entity) {
        if (entity.getPasswordHash() == null) {
            throw new IllegalStateException("PasswordHash cannot be null");
        }
    }

    /**
     * PasswordHash cannot be blank
     */
    public static void invariantPasswordHashNotBlank(User entity) {
        if (entity.getPasswordHash() == null || entity.getPasswordHash().trim().isEmpty()) {
            throw new IllegalStateException("PasswordHash cannot be blank");
        }
    }

    /**
     * ShippingAddress cannot be null
     */
    public static void invariantShippingAddressNotNull(User entity) {
        if (entity.getShippingAddress() == null) {
            throw new IllegalStateException("ShippingAddress cannot be null");
        }
    }

    /**
     * ShippingAddress cannot be blank
     */
    public static void invariantShippingAddressNotBlank(User entity) {
        if (entity.getShippingAddress() == null || entity.getShippingAddress().trim().isEmpty()) {
            throw new IllegalStateException("ShippingAddress cannot be blank");
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