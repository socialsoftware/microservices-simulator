package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.Post;

/**
 * Invariant validation methods for Post
 */
public class PostInvariants {

    /**
     * Title cannot be null
     */
    public static void invariantTitleNotNull(Post entity) {
        if (entity.getTitle() == null) {
            throw new IllegalStateException("Title cannot be null");
        }
    }

    /**
     * Title cannot be blank
     */
    public static void invariantTitleNotBlank(Post entity) {
        if (entity.getTitle() == null || entity.getTitle().trim().isEmpty()) {
            throw new IllegalStateException("Title cannot be blank");
        }
    }

    /**
     * Content cannot be null
     */
    public static void invariantContentNotNull(Post entity) {
        if (entity.getContent() == null) {
            throw new IllegalStateException("Content cannot be null");
        }
    }

    /**
     * Content cannot be blank
     */
    public static void invariantContentNotBlank(Post entity) {
        if (entity.getContent() == null || entity.getContent().trim().isEmpty()) {
            throw new IllegalStateException("Content cannot be blank");
        }
    }

    /**
     * Author cannot be null
     */
    public static void invariantAuthorNotNull(Post entity) {
        if (entity.getAuthor() == null) {
            throw new IllegalStateException("Author cannot be null");
        }
    }

    /**
     * PublishedAt cannot be null
     */
    public static void invariantPublishedAtNotNull(Post entity) {
        if (entity.getPublishedAt() == null) {
            throw new IllegalStateException("PublishedAt cannot be null");
        }
    }

    /**
     * Post aggregate must be in a valid state
     */
    public static void invariantPostValid(Post entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}