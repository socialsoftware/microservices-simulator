package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.utils.DateHandler;

import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Event {
    @Id
    @GeneratedValue
    private Integer id;
    @Column
    private Integer publisherAggregateId;
    @Column
    private Integer publisherAggregateVersion;
    private LocalDateTime timestamp;

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

    public Integer getPublisherAggregateVersion() {
        return publisherAggregateVersion;
    }

    public void setPublisherAggregateVersion(Integer publisherAggregateVersion) {
        this.publisherAggregateVersion = publisherAggregateVersion;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

}
