package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;

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
     * QuizType cannot be null
     */
    public static void invariantQuizTypeNotNull(Quiz entity) {
        if (entity.getQuizType() == null) {
            throw new IllegalStateException("QuizType cannot be null");
        }
    }

    /**
     * CreationDate cannot be null
     */
    public static void invariantCreationDateNotNull(Quiz entity) {
        if (entity.getCreationDate() == null) {
            throw new IllegalStateException("CreationDate cannot be null");
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
     * ResultsDate cannot be null
     */
    public static void invariantResultsDateNotNull(Quiz entity) {
        if (entity.getResultsDate() == null) {
            throw new IllegalStateException("ResultsDate cannot be null");
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
     * Quiz aggregate must be in a valid state
     */
    public static void invariantQuizValid(Quiz entity) {
        // Aggregate-level validation logic
        // Validate business rules that span multiple properties
        // Example: startDate must be before endDate
        // Note: Aggregate-specific invariants should be defined in DSL invariants block
    }

}