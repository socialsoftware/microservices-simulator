package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;

public class TopicDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private TopicCourseDto course;

    public TopicDto() {
    }

    public TopicDto(Topic topic) {
        this.aggregateId = topic.getAggregateId();
        this.version = topic.getVersion();
        this.state = topic.getState();
        this.name = topic.getName();
        this.course = topic.getCourse() != null ? new TopicCourseDto(topic.getCourse()) : null;
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TopicCourseDto getCourse() {
        return course;
    }

    public void setCourse(TopicCourseDto course) {
        this.course = course;
    }
}