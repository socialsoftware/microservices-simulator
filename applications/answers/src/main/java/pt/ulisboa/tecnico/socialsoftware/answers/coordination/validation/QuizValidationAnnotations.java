package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.validation.annotations;

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
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.*;


/**
 * Validation annotations for Quiz properties
 */
public class QuizValidationAnnotations {

    /**
     * Validation annotations for title
     */
    public static class TitleValidation {
        @NotNull
    @NotBlank
        private String title;
        
        // Getter and setter
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
    }

    /**
     * Validation annotations for description
     */
    public static class DescriptionValidation {
        @NotNull
    @NotBlank
        private String description;
        
        // Getter and setter
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * Validation annotations for quizType
     */
    public static class QuizTypeValidation {
        @NotNull
    @NotBlank
        private String quizType;
        
        // Getter and setter
        public String getQuizType() {
            return quizType;
        }
        
        public void setQuizType(String quizType) {
            this.quizType = quizType;
        }
    }

    /**
     * Validation annotations for availableDate
     */
    public static class AvailableDateValidation {
        @NotNull
        private LocalDateTime availableDate;
        
        // Getter and setter
        public LocalDateTime getAvailableDate() {
            return availableDate;
        }
        
        public void setAvailableDate(LocalDateTime availableDate) {
            this.availableDate = availableDate;
        }
    }

    /**
     * Validation annotations for conclusionDate
     */
    public static class ConclusionDateValidation {
        @NotNull
        private LocalDateTime conclusionDate;
        
        // Getter and setter
        public LocalDateTime getConclusionDate() {
            return conclusionDate;
        }
        
        public void setConclusionDate(LocalDateTime conclusionDate) {
            this.conclusionDate = conclusionDate;
        }
    }

    /**
     * Validation annotations for numberOfQuestions
     */
    public static class NumberOfQuestionsValidation {
        @NotNull
        private Integer numberOfQuestions;
        
        // Getter and setter
        public Integer getNumberOfQuestions() {
            return numberOfQuestions;
        }
        
        public void setNumberOfQuestions(Integer numberOfQuestions) {
            this.numberOfQuestions = numberOfQuestions;
        }
    }

    /**
     * Validation annotations for execution
     */
    public static class ExecutionValidation {
        @NotNull
        private QuizExecution execution;
        
        // Getter and setter
        public QuizExecution getExecution() {
            return execution;
        }
        
        public void setExecution(QuizExecution execution) {
            this.execution = execution;
        }
    }

    /**
     * Validation annotations for questions
     */
    public static class QuestionsValidation {
        @NotNull
    @NotEmpty
        private Set<QuizQuestion> questions;
        
        // Getter and setter
        public Set<QuizQuestion> getQuestions() {
            return questions;
        }
        
        public void setQuestions(Set<QuizQuestion> questions) {
            this.questions = questions;
        }
    }

    /**
     * Validation annotations for options
     */
    public static class OptionsValidation {
        @NotNull
    @NotEmpty
        private Set<QuizOption> options;
        
        // Getter and setter
        public Set<QuizOption> getOptions() {
            return options;
        }
        
        public void setOptions(Set<QuizOption> options) {
            this.options = options;
        }
    }

}