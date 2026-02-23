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
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.Enrollment;

/**
 * Invariant validation methods for Enrollment
 */
public class EnrollmentInvariants {

    /**
     * Course cannot be null
     */
    public static void invariantCourseNotNull(Enrollment entity) {
        if (entity.getCourse() == null) {
            throw new IllegalStateException("Course cannot be null");
        }
    }

    /**
     * Teachers cannot be null
     */
    public static void invariantTeachersNotNull(Enrollment entity) {
        if (entity.getTeachers() == null) {
            throw new IllegalStateException("Teachers cannot be null");
        }
    }

    /**
     * Teachers cannot be empty
     */
    public static void invariantTeachersNotEmpty(Enrollment entity) {
        if (entity.getTeachers() == null || ((java.util.Collection) entity.getTeachers()).isEmpty()) {
            throw new IllegalStateException("Teachers cannot be empty");
        }
    }

    /**
     * EnrollmentDate cannot be null
     */
    public static void invariantEnrollmentDateNotNull(Enrollment entity) {
        if (entity.getEnrollmentDate() == null) {
            throw new IllegalStateException("EnrollmentDate cannot be null");
        }
    }

    /**
     * Active cannot be null
     */
    public static void invariantActiveNotNull(Enrollment entity) {
        if (entity.getActive() == null) {
            throw new IllegalStateException("Active cannot be null");
        }
    }

    /**
     * Enrollment aggregate must be in a valid state
     */
    public static void invariantEnrollmentValid(Enrollment entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}