package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.validation.invariants;


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

/**
 * Invariant validation methods for Tournament
 */
public class TournamentInvariants {

    /**
     * StartTime cannot be null
     */
    public static void invariantStartTimeNotNull(Tournament entity) {
        if (entity.getStartTime() == null) {
            throw new IllegalStateException("StartTime cannot be null");
        }
    }

    /**
     * EndTime cannot be null
     */
    public static void invariantEndTimeNotNull(Tournament entity) {
        if (entity.getEndTime() == null) {
            throw new IllegalStateException("EndTime cannot be null");
        }
    }

    /**
     * NumberOfQuestions cannot be null
     */
    public static void invariantNumberOfQuestionsNotNull(Tournament entity) {
        if (entity.getNumberOfQuestions() == null) {
            throw new IllegalStateException("NumberOfQuestions cannot be null");
        }
    }

    /**
     * Cancelled cannot be null
     */
    public static void invariantCancelledNotNull(Tournament entity) {
        if (entity.isCancelled() == null) {
            throw new IllegalStateException("Cancelled cannot be null");
        }
    }

    /**
     * Creator cannot be null
     */
    public static void invariantCreatorNotNull(Tournament entity) {
        if (entity.getCreator() == null) {
            throw new IllegalStateException("Creator cannot be null");
        }
    }

    /**
     * Participants cannot be null
     */
    public static void invariantParticipantsNotNull(Tournament entity) {
        if (entity.getParticipants() == null) {
            throw new IllegalStateException("Participants cannot be null");
        }
    }

    /**
     * Participants cannot be empty
     */
    public static void invariantParticipantsNotEmpty(Tournament entity) {
        if (entity.getParticipants() == null || ((java.util.Collection) entity.getParticipants()).isEmpty()) {
            throw new IllegalStateException("Participants cannot be empty");
        }
    }

    /**
     * Execution cannot be null
     */
    public static void invariantExecutionNotNull(Tournament entity) {
        if (entity.getExecution() == null) {
            throw new IllegalStateException("Execution cannot be null");
        }
    }

    /**
     * Topics cannot be null
     */
    public static void invariantTopicsNotNull(Tournament entity) {
        if (entity.getTopics() == null) {
            throw new IllegalStateException("Topics cannot be null");
        }
    }

    /**
     * Topics cannot be empty
     */
    public static void invariantTopicsNotEmpty(Tournament entity) {
        if (entity.getTopics() == null || ((java.util.Collection) entity.getTopics()).isEmpty()) {
            throw new IllegalStateException("Topics cannot be empty");
        }
    }

    /**
     * Quiz cannot be null
     */
    public static void invariantQuizNotNull(Tournament entity) {
        if (entity.getQuiz() == null) {
            throw new IllegalStateException("Quiz cannot be null");
        }
    }

    /**
     * Tournament aggregate must be in a valid state
     */
    public static void invariantTournamentValid(Tournament entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}