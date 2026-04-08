package pt.ulisboa.tecnico.socialsoftware.ms.notification;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;

import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", length = 50) // increase size if needed
public abstract class Event {
    @Id
    @GeneratedValue
    private Integer id;
    @Column
    private Integer publisherAggregateId;
    @Column
    private Long publisherAggregateVersion;
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean published = false;

    public Event() {

    }

    public Event(Integer publisherAggregateId) {
        setPublisherAggregateId(publisherAggregateId);
        setTimestamp(DateHandler.now());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPublisherAggregateId() {
        return publisherAggregateId;
    }

    public void setPublisherAggregateId(Integer publisherAggregateId) {
        this.publisherAggregateId = publisherAggregateId;
    }

    public Long getPublisherAggregateVersion() {
        return publisherAggregateVersion;
    }

    public void setPublisherAggregateVersion(Long publisherAggregateVersion) {
        this.publisherAggregateVersion = publisherAggregateVersion;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

}
