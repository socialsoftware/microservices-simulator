package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.MergeableAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

/*
    INTRA-INVARIANTS:

    INTER-INVARIANTS:
        COURSE-EXISTS (course doesnt send events)
 */
@Entity
public abstract class Topic extends Aggregate implements MergeableAggregate {
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
    public void verifyInvariants() {
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

    public TopicCourse getTopicCourse() {
        return topicCourse;
    }

    public void setTopicCourse(TopicCourse course) {
        this.topicCourse = course;
        this.topicCourse.setTopic(this);
    }

    @Override
    public Set<String> getMutableFields() {
        return Set.of("name");
    }

    @Override
    public Set<String[]> getIntentions() {
        return new HashSet<>();
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
        Topic committedTopic = (Topic) committedVersion;
        mergeName(toCommitVersionChangedFields, this, committedTopic);
        return this;
    }

    private void mergeName(Set<String> toCommitVersionChangedFields, Topic mergedTopic, Topic committedTopic) {
        if (toCommitVersionChangedFields.contains("name")) {
            mergedTopic.setName(getName());
        } else {
            mergedTopic.setName(committedTopic.getName());
        }
    }

}
