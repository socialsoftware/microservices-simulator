package pt.ulisboa.tecnico.socialsoftware.answers.coordination.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;

public class QuizValidationAnnotations {

    public static class TitleValidation {
        @NotNull
    @NotBlank
        private String title;
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
    }

    public static class QuizTypeValidation {
        @NotNull
        private QuizType quizType;
        
        public QuizType getQuizType() {
            return quizType;
        }
        
        public void setQuizType(QuizType quizType) {
            this.quizType = quizType;
        }
    }

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

    public static class AvailableDateValidation {
        @NotNull
        private LocalDateTime availableDate;
        
        public LocalDateTime getAvailableDate() {
            return availableDate;
        }
        
        public void setAvailableDate(LocalDateTime availableDate) {
            this.availableDate = availableDate;
        }
    }

    public static class ConclusionDateValidation {
        @NotNull
        private LocalDateTime conclusionDate;
        
        public LocalDateTime getConclusionDate() {
            return conclusionDate;
        }
        
        public void setConclusionDate(LocalDateTime conclusionDate) {
            this.conclusionDate = conclusionDate;
        }
    }

    public static class ResultsDateValidation {
        @NotNull
        private LocalDateTime resultsDate;
        
        public LocalDateTime getResultsDate() {
            return resultsDate;
        }
        
        public void setResultsDate(LocalDateTime resultsDate) {
            this.resultsDate = resultsDate;
        }
    }

    public static class ExecutionValidation {
        @NotNull
        private QuizExecution execution;
        
        public QuizExecution getExecution() {
            return execution;
        }
        
        public void setExecution(QuizExecution execution) {
            this.execution = execution;
        }
    }

    public static class QuestionsValidation {
        @NotNull
    @NotEmpty
        private Set<QuizQuestion> questions;
        
        public Set<QuizQuestion> getQuestions() {
            return questions;
        }
        
        public void setQuestions(Set<QuizQuestion> questions) {
            this.questions = questions;
        }
    }

}