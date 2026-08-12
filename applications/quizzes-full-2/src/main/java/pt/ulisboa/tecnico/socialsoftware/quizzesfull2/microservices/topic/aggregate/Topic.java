package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "topics")
public abstract class Topic extends Aggregate {
    private String name;
    private final Integer courseAggregateId;

    public Topic() {
        this.courseAggregateId = null;
    }

    public Topic(Integer aggregateId, String name, Integer courseAggregateId) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.name = name;
        this.courseAggregateId = courseAggregateId;
    }

    public Topic(Topic other) {
        super(other);
        this.name = other.getName();
        this.courseAggregateId = other.getCourseAggregateId();
    }

    @Override
    public void verifyInvariants() {
        // Topic has no P1 rule: neither §3.1 nor §3.2 of the domain model names one for this
        // aggregate, and the immutable Course reference is enforced by the `final` field above.
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }
}
