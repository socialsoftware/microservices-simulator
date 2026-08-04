package pt.ulisboa.tecnico.socialsoftware.trainticket.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.Contacts;

/**
 * Invariant validation methods for Contacts
 */
public class ContactsInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(Contacts entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(Contacts entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * DocumentType cannot be null
     */
    public static void invariantDocumentTypeNotNull(Contacts entity) {
        if (entity.getDocumentType() == null) {
            throw new IllegalStateException("DocumentType cannot be null");
        }
    }

    /**
     * DocumentNumber cannot be null
     */
    public static void invariantDocumentNumberNotNull(Contacts entity) {
        if (entity.getDocumentNumber() == null) {
            throw new IllegalStateException("DocumentNumber cannot be null");
        }
    }

    /**
     * DocumentNumber cannot be blank
     */
    public static void invariantDocumentNumberNotBlank(Contacts entity) {
        if (entity.getDocumentNumber() == null || entity.getDocumentNumber().trim().isEmpty()) {
            throw new IllegalStateException("DocumentNumber cannot be blank");
        }
    }

    /**
     * PhoneNumber cannot be null
     */
    public static void invariantPhoneNumberNotNull(Contacts entity) {
        if (entity.getPhoneNumber() == null) {
            throw new IllegalStateException("PhoneNumber cannot be null");
        }
    }

    /**
     * PhoneNumber cannot be blank
     */
    public static void invariantPhoneNumberNotBlank(Contacts entity) {
        if (entity.getPhoneNumber() == null || entity.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalStateException("PhoneNumber cannot be blank");
        }
    }

    /**
     * User cannot be null
     */
    public static void invariantUserNotNull(Contacts entity) {
        if (entity.getUser() == null) {
            throw new IllegalStateException("User cannot be null");
        }
    }

    /**
     * Contacts aggregate must be in a valid state
     */
    public static void invariantContactsValid(Contacts entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}