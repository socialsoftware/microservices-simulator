package pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate;

import static pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState.DELETED;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Aggregate {
    public enum AggregateState {
        ACTIVE,
        INACTIVE,
        DELETED
    }

    @Id
    @GeneratedValue
    private Integer id;
    private Integer aggregateId;
    @Column
    private Integer version;
    private LocalDateTime creationTs;
    @Enumerated(EnumType.STRING)
    private AggregateState state;
    private String aggregateType;
    @ManyToOne
    private Aggregate prev;

    public void remove() {
        setState(DELETED);
    }

    public Aggregate() {

    }

    /* used when creating a new aggregate*/
    public Aggregate(Integer aggregateId) {
        setAggregateId(aggregateId);
        setState(AggregateState.ACTIVE);
        setPrev(null);
    }

    public Aggregate(Aggregate other) {
        setId(null);
        setAggregateId(other.getAggregateId());
        setAggregateType(other.getAggregateType());
        setState(other.getState());
        setPrev(other);
    }

    public abstract void verifyInvariants();

    public abstract Set<EventSubscription> getEventSubscriptions();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreationTs() {
        return creationTs;
    }

    public void setCreationTs(LocalDateTime creationTs) {
        this.creationTs = creationTs;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public Aggregate getPrev() {
        return this.prev;
    }

    public void setPrev(Aggregate prev) {
        this.prev = prev;
    }

    public Set<EventSubscription> getEventSubscriptionsByEventType(String eventType) {
        return getEventSubscriptions().stream()
                .filter(es -> es.getEventType().equals(eventType))
                .collect(Collectors.toSet());
    }

}
