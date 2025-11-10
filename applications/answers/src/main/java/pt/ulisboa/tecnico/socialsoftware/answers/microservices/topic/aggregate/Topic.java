package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicCourse;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;

@Entity
public abstract class Topic extends Aggregate {
    private String name;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "topic")
    private TopicCourse course;

    public Topic() {

    }

    public Topic(Integer aggregateId, TopicDto topicDto, TopicCourse course) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(topicDto.getName());
        setCourse(course);
    }

    public Topic(Topic other) {
        super(other);
        setName(other.getName());
        setCourse(new TopicCourse(other.getCourse()));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TopicCourse getCourse() {
        return course;
    }

    public void setCourse(TopicCourse course) {
        this.course = course;
        if (this.course != null) {
            this.course.setTopic(this);
        }
    }


    @Override
    public void verifyInvariants() {
        // No invariants defined
    }
}