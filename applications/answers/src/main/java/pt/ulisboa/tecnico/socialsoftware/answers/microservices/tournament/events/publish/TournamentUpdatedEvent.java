package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;

@Entity
public class TournamentUpdatedEvent extends Event {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;

    public TournamentUpdatedEvent() {
    }

    public TournamentUpdatedEvent(Integer aggregateId, LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions, Boolean cancelled) {
        super(aggregateId);
        setStartTime(startTime);
        setEndTime(endTime);
        setNumberOfQuestions(numberOfQuestions);
        setCancelled(cancelled);
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

}