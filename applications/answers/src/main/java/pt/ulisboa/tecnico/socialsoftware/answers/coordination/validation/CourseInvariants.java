package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.validation.invariants;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.*;


/**
 * Invariant validation methods for Course
 */
public class CourseInvariants {

    /**
     * Name cannot be null
     */
    public static void invariantNameNotNull(Course entity) {
        if (entity.getName() == null) {
            throw new IllegalStateException("Name cannot be null");
        }
    }

    /**
     * Name cannot be blank
     */
    public static void invariantNameNotBlank(Course entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalStateException("Name cannot be blank");
        }
    }

    /**
     * Type cannot be null
     */
    public static void invariantTypeNotNull(Course entity) {
        if (entity.getType() == null) {
            throw new IllegalStateException("Type cannot be null");
        }
    }

    /**
     * CreationDate cannot be null
     */
    public static void invariantCreationDateNotNull(Course entity) {
        if (entity.getCreationDate() == null) {
            throw new IllegalStateException("CreationDate cannot be null");
        }
    }

    /**
     * Course aggregate must be in a valid state
     */
    public static void invariantCourseValid(Course entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // TODO: Implement aggregate-specific business rules
    }

}