package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;
import jakarta.persistence.Column;

@Entity
public class TournamentUpdatedEvent extends Event {
    @Column(name = "tournament_updated_event_start_time")
    private LocalDateTime startTime;
    @Column(name = "tournament_updated_event_end_time")
    private LocalDateTime endTime;
    @Column(name = "tournament_updated_event_number_of_questions")
    private Integer numberOfQuestions;
    @Column(name = "tournament_updated_event_cancelled")
    private Boolean cancelled;

    public TournamentUpdatedEvent() {
        super();
    }

    public TournamentUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
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