package pt.ulisboa.tecnico.socialsoftware.eventdriven.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class PostAuthorUpdatedEvent extends Event {
    private Integer authorAggregateId;
    private Integer authorVersion;
    private String authorName;

    public PostAuthorUpdatedEvent() {
        super();
    }

    public PostAuthorUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PostAuthorUpdatedEvent(Integer aggregateId, Integer authorAggregateId, Integer authorVersion, String authorName) {
        super(aggregateId);
        setAuthorAggregateId(authorAggregateId);
        setAuthorVersion(authorVersion);
        setAuthorName(authorName);
    }

    public Integer getAuthorAggregateId() {
        return authorAggregateId;
    }

    public void setAuthorAggregateId(Integer authorAggregateId) {
        this.authorAggregateId = authorAggregateId;
    }

    public Integer getAuthorVersion() {
        return authorVersion;
    }

    public void setAuthorVersion(Integer authorVersion) {
        this.authorVersion = authorVersion;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

}