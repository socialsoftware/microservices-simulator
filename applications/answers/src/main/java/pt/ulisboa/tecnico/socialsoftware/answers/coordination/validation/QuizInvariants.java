package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.validation.invariants;

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

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.*;


/**
 * Invariant validation methods for Quiz
 */
public class QuizInvariants {

    /**
     * Title cannot be null
     */
    public static void invariantTitleNotNull(Quiz entity) {
        if (entity.getTitle() == null) {
            throw new IllegalStateException("Title cannot be null");
        }
    }

    /**
     * Title cannot be blank
     */
    public static void invariantTitleNotBlank(Quiz entity) {
        if (entity.getTitle() == null || entity.getTitle().trim().isEmpty()) {
            throw new IllegalStateException("Title cannot be blank");
        }
    }

    /**
     * Description cannot be null
     */
    public static void invariantDescriptionNotNull(Quiz entity) {
        if (entity.getDescription() == null) {
            throw new IllegalStateException("Description cannot be null");
        }
    }

    /**
     * Description cannot be blank
     */
    public static void invariantDescriptionNotBlank(Quiz entity) {
        if (entity.getDescription() == null || entity.getDescription().trim().isEmpty()) {
            throw new IllegalStateException("Description cannot be blank");
        }
    }

    /**
     * QuizType cannot be null
     */
    public static void invariantQuizTypeNotNull(Quiz entity) {
        if (entity.getQuizType() == null) {
            throw new IllegalStateException("QuizType cannot be null");
        }
    }

    /**
     * AvailableDate cannot be null
     */
    public static void invariantAvailableDateNotNull(Quiz entity) {
        if (entity.getAvailableDate() == null) {
            throw new IllegalStateException("AvailableDate cannot be null");
        }
    }

    /**
     * ConclusionDate cannot be null
     */
    public static void invariantConclusionDateNotNull(Quiz entity) {
        if (entity.getConclusionDate() == null) {
            throw new IllegalStateException("ConclusionDate cannot be null");
        }
    }

    /**
     * NumberOfQuestions cannot be null
     */
    public static void invariantNumberOfQuestionsNotNull(Quiz entity) {
        if (entity.getNumberOfQuestions() == null) {
            throw new IllegalStateException("NumberOfQuestions cannot be null");
        }
    }

    /**
     * Execution cannot be null
     */
    public static void invariantExecutionNotNull(Quiz entity) {
        if (entity.getExecution() == null) {
            throw new IllegalStateException("Execution cannot be null");
        }
    }

    /**
     * Questions cannot be null
     */
    public static void invariantQuestionsNotNull(Quiz entity) {
        if (entity.getQuestions() == null) {
            throw new IllegalStateException("Questions cannot be null");
        }
    }

    /**
     * Questions cannot be empty
     */
    public static void invariantQuestionsNotEmpty(Quiz entity) {
        if (entity.getQuestions() == null || ((java.util.Collection) entity.getQuestions()).isEmpty()) {
            throw new IllegalStateException("Questions cannot be empty");
        }
    }

    /**
     * Options cannot be null
     */
    public static void invariantOptionsNotNull(Quiz entity) {
        if (entity.getOptions() == null) {
            throw new IllegalStateException("Options cannot be null");
        }
    }

    /**
     * Options cannot be empty
     */
    public static void invariantOptionsNotEmpty(Quiz entity) {
        if (entity.getOptions() == null || ((java.util.Collection) entity.getOptions()).isEmpty()) {
            throw new IllegalStateException("Options cannot be empty");
        }
    }

    /**
     * Quiz aggregate must be in a valid state
     */
    public static void invariantQuizValid(Quiz entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // TODO: Implement aggregate-specific business rules
    }

}