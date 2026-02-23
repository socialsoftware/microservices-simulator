package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;


import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuestion;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerUser;

public class AnswerValidationAnnotations {

    public static class CreationDateValidation {
        @NotNull
        private LocalDateTime creationDate;
        
        public LocalDateTime getCreationDate() {
            return creationDate;
        }
        
        public void setCreationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
        }
    }

    public static class AnswerDateValidation {
        @NotNull
        private LocalDateTime answerDate;
        
        public LocalDateTime getAnswerDate() {
            return answerDate;
        }
        
        public void setAnswerDate(LocalDateTime answerDate) {
            this.answerDate = answerDate;
        }
    }

    public static class CompletedValidation {
        @NotNull
        private Boolean completed;
        
        public Boolean getCompleted() {
            return completed;
        }
        
        public void setCompleted(Boolean completed) {
            this.completed = completed;
        }
    }

    public static class ExecutionValidation {
        @NotNull
        private AnswerExecution execution;
        
        public AnswerExecution getExecution() {
            return execution;
        }
        
        public void setExecution(AnswerExecution execution) {
            this.execution = execution;
        }
    }

    public static class UserValidation {
        @NotNull
        private AnswerUser user;
        
        public AnswerUser getUser() {
            return user;
        }
        
        public void setUser(AnswerUser user) {
            this.user = user;
        }
    }

    public static class QuizValidation {
        @NotNull
        private AnswerQuiz quiz;
        
        public AnswerQuiz getQuiz() {
            return quiz;
        }
        
        public void setQuiz(AnswerQuiz quiz) {
            this.quiz = quiz;
        }
    }

    public static class QuestionsValidation {
        @NotNull
    @NotEmpty
        private List<AnswerQuestion> questions;
        
        public List<AnswerQuestion> getQuestions() {
            return questions;
        }
        
        public void setQuestions(List<AnswerQuestion> questions) {
            this.questions = questions;
        }
    }

}