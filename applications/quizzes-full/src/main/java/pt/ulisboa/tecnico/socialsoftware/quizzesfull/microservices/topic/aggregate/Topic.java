package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;

import java.util.HashSet;
import java.util.Set;

/*
    INTRA-INVARIANTS:
        (none)
    INTER-INVARIANTS:
        (wired in Phase 4 via /wire-event)
*/
@Entity
public abstract class Topic extends Aggregate {

    @Column
    private String name;

    // --- Snapshot fields ---
    @Column
    private Integer courseId;

    public Topic() {
    }

    public Topic(Integer aggregateId, TopicDto topicDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.name = topicDto.getName();
        this.courseId = topicDto.getCourseId();
    }

    public Topic(Topic other) {
        super(other);
        this.name = other.getName();
        this.courseId = other.getCourseId();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public void verifyInvariants() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }
}
