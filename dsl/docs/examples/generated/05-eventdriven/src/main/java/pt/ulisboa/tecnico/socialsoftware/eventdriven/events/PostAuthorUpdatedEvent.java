package pt.ulisboa.tecnico.socialsoftware.eventdriven.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class PostAuthorUpdatedEvent extends Event {
    @Column(name = "post_author_updated_event_author_aggregate_id")
    private Integer authorAggregateId;
    @Column(name = "post_author_updated_event_author_version")
    private Integer authorVersion;
    @Column(name = "post_author_updated_event_author_name")
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