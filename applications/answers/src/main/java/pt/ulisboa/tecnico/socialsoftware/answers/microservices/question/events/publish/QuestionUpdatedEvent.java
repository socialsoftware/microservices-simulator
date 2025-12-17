package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuestionUpdatedEvent extends Event {
    private String title;
    private String content;
    private LocalDateTime creationDate;

    public QuestionUpdatedEvent() {
    }

    public QuestionUpdatedEvent(Integer aggregateId, String title, String content, LocalDateTime creationDate) {
        super(aggregateId);
        setTitle(title);
        setContent(content);
        setCreationDate(creationDate);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

}