package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.validation;


import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.*;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.regex.Pattern;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.Book;

/**
 * Invariant validation methods for Book
 */
public class BookInvariants {

    /**
     * Title cannot be null
     */
    public static void invariantTitleNotNull(Book entity) {
        if (entity.getTitle() == null) {
            throw new IllegalStateException("Title cannot be null");
        }
    }

    /**
     * Title cannot be blank
     */
    public static void invariantTitleNotBlank(Book entity) {
        if (entity.getTitle() == null || entity.getTitle().trim().isEmpty()) {
            throw new IllegalStateException("Title cannot be blank");
        }
    }

    /**
     * Author cannot be null
     */
    public static void invariantAuthorNotNull(Book entity) {
        if (entity.getAuthor() == null) {
            throw new IllegalStateException("Author cannot be null");
        }
    }

    /**
     * Author cannot be blank
     */
    public static void invariantAuthorNotBlank(Book entity) {
        if (entity.getAuthor() == null || entity.getAuthor().trim().isEmpty()) {
            throw new IllegalStateException("Author cannot be blank");
        }
    }

    /**
     * Genre cannot be null
     */
    public static void invariantGenreNotNull(Book entity) {
        if (entity.getGenre() == null) {
            throw new IllegalStateException("Genre cannot be null");
        }
    }

    /**
     * Genre cannot be blank
     */
    public static void invariantGenreNotBlank(Book entity) {
        if (entity.getGenre() == null || entity.getGenre().trim().isEmpty()) {
            throw new IllegalStateException("Genre cannot be blank");
        }
    }

    /**
     * Available cannot be null
     */
    public static void invariantAvailableNotNull(Book entity) {
        if (entity.getAvailable() == null) {
            throw new IllegalStateException("Available cannot be null");
        }
    }

    /**
     * Book aggregate must be in a valid state
     */
    public static void invariantBookValid(Book entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}