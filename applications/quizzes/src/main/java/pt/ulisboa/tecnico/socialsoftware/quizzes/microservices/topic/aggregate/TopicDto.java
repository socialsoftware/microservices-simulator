package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TopicDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private Integer courseId;
    private String name;
    private AggregateState state;

    public TopicDto() {
    }

    public TopicDto(Topic topic) {
        setAggregateId(topic.getAggregateId());
        setVersion(topic.getVersion());
        setCourseId(topic.getTopicCourse().getCourseAggregateId());
        setName(topic.getName());
        setState(topic.getState());
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

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getAcronym() {
        return name;
    }

    public void setAcronym(String acronym) {
        this.name = acronym;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }
}
