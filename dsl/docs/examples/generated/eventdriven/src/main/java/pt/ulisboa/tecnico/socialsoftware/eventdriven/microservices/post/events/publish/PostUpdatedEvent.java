package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;

@Entity
public class PostUpdatedEvent extends Event {
    private String title;
    private String content;
    private LocalDateTime publishedAt;

    public PostUpdatedEvent() {
        super();
    }

    public PostUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PostUpdatedEvent(Integer aggregateId, String title, String content, LocalDateTime publishedAt) {
        super(aggregateId);
        setTitle(title);
        setContent(content);
        setPublishedAt(publishedAt);
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

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

}