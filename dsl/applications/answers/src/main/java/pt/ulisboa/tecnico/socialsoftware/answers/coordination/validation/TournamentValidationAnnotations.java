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
import java.util.Set;
import java.util.regex.Pattern;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentTopic;

public class TournamentValidationAnnotations {

    public static class StartTimeValidation {
        @NotNull
        private LocalDateTime startTime;
        
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }
    }

    public static class EndTimeValidation {
        @NotNull
        private LocalDateTime endTime;
        
        public LocalDateTime getEndTime() {
            return endTime;
        }
        
        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }
    }

    public static class NumberOfQuestionsValidation {
        @NotNull
        private Integer numberOfQuestions;
        
        public Integer getNumberOfQuestions() {
            return numberOfQuestions;
        }
        
        public void setNumberOfQuestions(Integer numberOfQuestions) {
            this.numberOfQuestions = numberOfQuestions;
        }
    }

    public static class CancelledValidation {
        @NotNull
        private Boolean cancelled;
        
        public Boolean getCancelled() {
            return cancelled;
        }
        
        public void setCancelled(Boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    public static class CreatorValidation {
        @NotNull
        private TournamentCreator creator;
        
        public TournamentCreator getCreator() {
            return creator;
        }
        
        public void setCreator(TournamentCreator creator) {
            this.creator = creator;
        }
    }

    public static class ParticipantsValidation {
        @NotNull
    @NotEmpty
        private Set<TournamentParticipant> participants;
        
        public Set<TournamentParticipant> getParticipants() {
            return participants;
        }
        
        public void setParticipants(Set<TournamentParticipant> participants) {
            this.participants = participants;
        }
    }

    public static class ExecutionValidation {
        @NotNull
        private TournamentExecution execution;
        
        public TournamentExecution getExecution() {
            return execution;
        }
        
        public void setExecution(TournamentExecution execution) {
            this.execution = execution;
        }
    }

    public static class TopicsValidation {
        @NotNull
    @NotEmpty
        private Set<TournamentTopic> topics;
        
        public Set<TournamentTopic> getTopics() {
            return topics;
        }
        
        public void setTopics(Set<TournamentTopic> topics) {
            this.topics = topics;
        }
    }

    public static class QuizValidation {
        @NotNull
        private TournamentQuiz quiz;
        
        public TournamentQuiz getQuiz() {
            return quiz;
        }
        
        public void setQuiz(TournamentQuiz quiz) {
            this.quiz = quiz;
        }
    }

}