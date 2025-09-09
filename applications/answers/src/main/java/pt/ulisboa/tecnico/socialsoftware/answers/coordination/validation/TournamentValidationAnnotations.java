package com.generated.microservices.answers.microservices.tournament.validation.annotations;

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

import com.generated.microservices.answers.microservices.tournament.aggregate.*;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


/**
 * Validation annotations for Tournament properties
 */
public class TournamentValidationAnnotations {

    /**
     * Validation annotations for startTime
     */
    public static class StartTimeValidation {
        @NotNull
        private LocalDateTime startTime;
        
        // Getter and setter
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }
    }

    /**
     * Validation annotations for endTime
     */
    public static class EndTimeValidation {
        @NotNull
        private LocalDateTime endTime;
        
        // Getter and setter
        public LocalDateTime getEndTime() {
            return endTime;
        }
        
        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
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
     * Validation annotations for cancelled
     */
    public static class CancelledValidation {
        @NotNull
        private Boolean cancelled;
        
        // Getter and setter
        public Boolean getCancelled() {
            return cancelled;
        }
        
        public void setCancelled(Boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /**
     * Validation annotations for tournamentCreator
     */
    public static class TournamentCreatorValidation {
        @NotNull
        private Object tournamentCreator;
        
        // Getter and setter
        public Object getTournamentCreator() {
            return tournamentCreator;
        }
        
        public void setTournamentCreator(Object tournamentCreator) {
            this.tournamentCreator = tournamentCreator;
        }
    }

    /**
     * Validation annotations for tournamentParticipants
     */
    public static class TournamentParticipantsValidation {
        @NotNull
    @NotEmpty
        private Object tournamentParticipants;
        
        // Getter and setter
        public Object getTournamentParticipants() {
            return tournamentParticipants;
        }
        
        public void setTournamentParticipants(Object tournamentParticipants) {
            this.tournamentParticipants = tournamentParticipants;
        }
    }

    /**
     * Validation annotations for tournamentCourseExecution
     */
    public static class TournamentCourseExecutionValidation {
        @NotNull
        private Object tournamentCourseExecution;
        
        // Getter and setter
        public Object getTournamentCourseExecution() {
            return tournamentCourseExecution;
        }
        
        public void setTournamentCourseExecution(Object tournamentCourseExecution) {
            this.tournamentCourseExecution = tournamentCourseExecution;
        }
    }

    /**
     * Validation annotations for tournamentTopics
     */
    public static class TournamentTopicsValidation {
        @NotNull
    @NotEmpty
        private Object tournamentTopics;
        
        // Getter and setter
        public Object getTournamentTopics() {
            return tournamentTopics;
        }
        
        public void setTournamentTopics(Object tournamentTopics) {
            this.tournamentTopics = tournamentTopics;
        }
    }

    /**
     * Validation annotations for tournamentQuiz
     */
    public static class TournamentQuizValidation {
        @NotNull
        private Object tournamentQuiz;
        
        // Getter and setter
        public Object getTournamentQuiz() {
            return tournamentQuiz;
        }
        
        public void setTournamentQuiz(Object tournamentQuiz) {
            this.tournamentQuiz = tournamentQuiz;
        }
    }

}