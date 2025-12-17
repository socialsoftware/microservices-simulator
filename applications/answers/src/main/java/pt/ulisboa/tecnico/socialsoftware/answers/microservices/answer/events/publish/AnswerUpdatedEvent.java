package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;

@Entity
public class AnswerUpdatedEvent extends Event {
    private LocalDateTime creationDate;
    private LocalDateTime answerDate;
    private Boolean completed;

    public AnswerUpdatedEvent() {
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