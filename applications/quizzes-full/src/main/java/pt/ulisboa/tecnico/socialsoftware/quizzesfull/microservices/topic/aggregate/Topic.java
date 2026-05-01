package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;

import java.util.HashSet;
import java.util.Set;

/*
    INTRA-INVARIANTS:
        (none)
    INTER-INVARIANTS:
        COURSE-EXISTS (course doesn't send events; no subscription needed)
 */
@Entity
public abstract class Topic extends Aggregate {

    @Column
    private String name;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "topic")
    private TopicCourse topicCourse;

    public Topic() {}

    public Topic(Integer aggregateId, String name, TopicCourse topicCourse) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(name);
        setTopicCourse(topicCourse);
    }

    public Topic(Topic other) {
        super(other);
        setName(other.getName());
        setTopicCourse(new TopicCourse(other.getTopicCourse()));
    }

    @Override
    public void verifyInvariants() {}

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

    public TopicCourse getTopicCourse() {
        return topicCourse;
    }

    public void setTopicCourse(TopicCourse topicCourse) {
        this.topicCourse = topicCourse;
        this.topicCourse.setTopic(this);
    }
}
