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
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.Teacher;

/**
 * Invariant validation methods for Teacher
 */
public class TeacherInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(Teacher entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(Teacher entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * Email cannot be null
     */
    public static void invariantEmailNotNull(Teacher entity) {
        if (entity.getEmail() == null) {
            throw new IllegalStateException("Email cannot be null");
        }
    }

    /**
     * Email cannot be blank
     */
    public static void invariantEmailNotBlank(Teacher entity) {
        if (entity.getEmail() == null || entity.getEmail().trim().isEmpty()) {
            throw new IllegalStateException("Email cannot be blank");
        }
    }

    /**
     * Email must be a valid email format
     */
    public static void invariantEmailEmailFormat(Teacher entity) {
        if (entity.getEmail() != null && !entity.getEmail().matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new IllegalStateException("Email must be a valid email format");
        }
    }

    /**
     * Department cannot be null
     */
    public static void invariantDepartmentNotNull(Teacher entity) {
        if (entity.getDepartment() == null) {
            throw new IllegalStateException("Department cannot be null");
        }
    }

    /**
     * Department cannot be blank
     */
    public static void invariantDepartmentNotBlank(Teacher entity) {
        if (entity.getDepartment() == null || entity.getDepartment().trim().isEmpty()) {
            throw new IllegalStateException("Department cannot be blank");
        }
    }

    /**
     * Teacher aggregate must be in a valid state
     */
    public static void invariantTeacherValid(Teacher entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}