package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;
import jakarta.persistence.Column;

@Entity
public class AnswerUpdatedEvent extends Event {
    @Column(name = "answer_updated_event_creation_date")
    private LocalDateTime creationDate;
    @Column(name = "answer_updated_event_answer_date")
    private LocalDateTime answerDate;
    @Column(name = "answer_updated_event_completed")
    private Boolean completed;

    public AnswerUpdatedEvent() {
        super();
    }

    public AnswerUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public AnswerUpdatedEvent(Integer aggregateId, LocalDateTime creationDate, LocalDateTime answerDate, Boolean completed) {
        super(aggregateId);
        setCreationDate(creationDate);
        setAnswerDate(answerDate);
        setCompleted(completed);
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getAnswerDate() {
        return answerDate;
    }

    public void setAnswerDate(LocalDateTime answerDate) {
        this.answerDate = answerDate;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

}