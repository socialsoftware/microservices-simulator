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
     * Validation annotations for completedDate
     */
    public static class CompletedDateValidation {
        @NotNull
        private LocalDateTime completedDate;
        
        // Getter and setter
        public LocalDateTime getCompletedDate() {
            return completedDate;
        }
        
        public void setCompletedDate(LocalDateTime completedDate) {
            this.completedDate = completedDate;
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
     * Validation annotations for quizAnswerStudent
     */
    public static class QuizAnswerStudentValidation {
        @NotNull
        private QuizAnswerStudent quizAnswerStudent;
        
        // Getter and setter
        public QuizAnswerStudent getQuizAnswerStudent() {
            return quizAnswerStudent;
        }
        
        public void setQuizAnswerStudent(QuizAnswerStudent quizAnswerStudent) {
            this.quizAnswerStudent = quizAnswerStudent;
        }
    }

    /**
     * Validation annotations for quizAnswerExecution
     */
    public static class QuizAnswerExecutionValidation {
        @NotNull
        private QuizAnswerExecution quizAnswerExecution;
        
        // Getter and setter
        public QuizAnswerExecution getQuizAnswerExecution() {
            return quizAnswerExecution;
        }
        
        public void setQuizAnswerExecution(QuizAnswerExecution quizAnswerExecution) {
            this.quizAnswerExecution = quizAnswerExecution;
        }
    }

    /**
     * Validation annotations for questionAnswers
     */
    public static class QuestionAnswersValidation {
        @NotNull
    @NotEmpty
        private Set<QuestionAnswer> questionAnswers;
        
        // Getter and setter
        public Set<QuestionAnswer> getQuestionAnswers() {
            return questionAnswers;
        }
        
        public void setQuestionAnswers(Set<QuestionAnswer> questionAnswers) {
            this.questionAnswers = questionAnswers;
        }
    }

    /**
     * Validation annotations for answeredQuiz
     */
    public static class AnsweredQuizValidation {
        @NotNull
        private AnsweredQuiz answeredQuiz;
        
        // Getter and setter
        public AnsweredQuiz getAnsweredQuiz() {
            return answeredQuiz;
        }
        
        public void setAnsweredQuiz(AnsweredQuiz answeredQuiz) {
            this.answeredQuiz = answeredQuiz;
        }
    }

}