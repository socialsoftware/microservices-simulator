package pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.validation;


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
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.Course;

/**
 * Invariant validation methods for Course
 */
public class CourseInvariants {

    /**
     * Title cannot be null
     */
    public static void invariantTitleNotNull(Course entity) {
        if (entity.getTitle() == null) {
            throw new IllegalStateException("Title cannot be null");
        }
    }

    /**
     * Title cannot be blank
     */
    public static void invariantTitleNotBlank(Course entity) {
        if (entity.getTitle() == null || entity.getTitle().trim().isEmpty()) {
            throw new IllegalStateException("Title cannot be blank");
        }
    }

    /**
     * Description cannot be null
     */
    public static void invariantDescriptionNotNull(Course entity) {
        if (entity.getDescription() == null) {
            throw new IllegalStateException("Description cannot be null");
        }
    }

    /**
     * Description cannot be blank
     */
    public static void invariantDescriptionNotBlank(Course entity) {
        if (entity.getDescription() == null || entity.getDescription().trim().isEmpty()) {
            throw new IllegalStateException("Description cannot be blank");
        }
    }

    /**
     * MaxStudents cannot be null
     */
    public static void invariantMaxStudentsNotNull(Course entity) {
        if (entity.getMaxStudents() == null) {
            throw new IllegalStateException("MaxStudents cannot be null");
        }
    }

    /**
     * Teacher cannot be null
     */
    public static void invariantTeacherNotNull(Course entity) {
        if (entity.getTeacher() == null) {
            throw new IllegalStateException("Teacher cannot be null");
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