package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.validation.annotations;

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

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.*;


/**
 * Validation annotations for Answer properties
 */
public class AnswerValidationAnnotations {

    /**
     * Validation annotations for creationDate
     */
    public static class CreationDateValidation {
        @NotNull
        private LocalDateTime creationDate;
        
        // Getter and setter
        public LocalDateTime getCreationDate() {
            return creationDate;
        }
        
        public void setCreationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
        }
    }

    /**
     * Validation annotations for answerDate
     */
    public static class AnswerDateValidation {
        @NotNull
        private LocalDateTime answerDate;
        
        // Getter and setter
        public LocalDateTime getAnswerDate() {
            return answerDate;
        }
        
        public void setAnswerDate(LocalDateTime answerDate) {
            this.answerDate = answerDate;
        }
    }

    /**
     * Validation annotations for completed
     */
    public static class CompletedValidation {
        @NotNull
        private Boolean completed;
        
        // Getter and setter
        public Boolean getCompleted() {
            return completed;
        }
        
        public void setCompleted(Boolean completed) {
            this.completed = completed;
        }
    }

    /**
     * Validation annotations for answerExecution
     */
    public static class AnswerExecutionValidation {
        @NotNull
        private AnswerExecution answerExecution;
        
        // Getter and setter
        public AnswerExecution getAnswerExecution() {
            return answerExecution;
        }
        
        public void setAnswerExecution(AnswerExecution answerExecution) {
            this.answerExecution = answerExecution;
        }
    }

    /**
     * Validation annotations for answerUser
     */
    public static class AnswerUserValidation {
        @NotNull
        private AnswerUser answerUser;
        
        // Getter and setter
        public AnswerUser getAnswerUser() {
            return answerUser;
        }
        
        public void setAnswerUser(AnswerUser answerUser) {
            this.answerUser = answerUser;
        }
    }

    /**
     * Validation annotations for answerQuiz
     */
    public static class AnswerQuizValidation {
        @NotNull
        private AnswerQuiz answerQuiz;
        
        // Getter and setter
        public AnswerQuiz getAnswerQuiz() {
            return answerQuiz;
        }
        
        public void setAnswerQuiz(AnswerQuiz answerQuiz) {
            this.answerQuiz = answerQuiz;
        }
    }

    /**
     * Validation annotations for answerQuestion
     */
    public static class AnswerQuestionValidation {
        @NotNull
    @NotEmpty
        private Set<AnswerQuestion> answerQuestion;
        
        // Getter and setter
        public Set<AnswerQuestion> getAnswerQuestion() {
            return answerQuestion;
        }
        
        public void setAnswerQuestion(Set<AnswerQuestion> answerQuestion) {
            this.answerQuestion = answerQuestion;
        }
    }

}