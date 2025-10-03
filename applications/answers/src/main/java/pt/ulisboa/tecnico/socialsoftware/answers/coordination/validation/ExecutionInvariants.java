package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.validation.invariants;

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

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;


/**
 * Invariant validation methods for Execution
 */
public class ExecutionInvariants {

    /**
     * Acronym cannot be null
     */
    public static void invariantAcronymNotNull(Execution entity) {
        if (entity.getAcronym() == null) {
            throw new IllegalStateException("Acronym cannot be null");
        }
    }

    /**
     * Acronym cannot be blank
     */
    public static void invariantAcronymNotBlank(Execution entity) {
        if (entity.getAcronym() == null || entity.getAcronym().trim().isEmpty()) {
            throw new IllegalStateException("Acronym cannot be blank");
        }
    }

    /**
     * AcademicTerm cannot be null
     */
    public static void invariantAcademicTermNotNull(Execution entity) {
        if (entity.getAcademicTerm() == null) {
            throw new IllegalStateException("AcademicTerm cannot be null");
        }
    }

    /**
     * AcademicTerm cannot be blank
     */
    public static void invariantAcademicTermNotBlank(Execution entity) {
        if (entity.getAcademicTerm() == null || entity.getAcademicTerm().trim().isEmpty()) {
            throw new IllegalStateException("AcademicTerm cannot be blank");
        }
    }

    /**
     * EndDate cannot be null
     */
    public static void invariantEndDateNotNull(Execution entity) {
        if (entity.getEndDate() == null) {
            throw new IllegalStateException("EndDate cannot be null");
        }
    }

    /**
     * ExecutionCourse cannot be null
     */
    public static void invariantExecutionCourseNotNull(Execution entity) {
        if (entity.getExecutionCourse() == null) {
            throw new IllegalStateException("ExecutionCourse cannot be null");
        }
    }

    /**
     * Students cannot be null
     */
    public static void invariantStudentsNotNull(Execution entity) {
        if (entity.getStudents() == null) {
            throw new IllegalStateException("Students cannot be null");
        }
    }

    /**
     * Students cannot be empty
     */
    public static void invariantStudentsNotEmpty(Execution entity) {
        if (entity.getStudents() == null || ((java.util.Collection) entity.getStudents()).isEmpty()) {
            throw new IllegalStateException("Students cannot be empty");
        }
    }

    /**
     * Execution aggregate must be in a valid state
     */
    public static void invariantExecutionValid(Execution entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // TODO: Implement aggregate-specific business rules
    }

}