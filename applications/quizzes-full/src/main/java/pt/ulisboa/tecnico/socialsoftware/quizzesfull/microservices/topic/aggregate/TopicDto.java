package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;
import java.io.Serializable;

public class TopicDto implements Serializable {
    private Integer aggregateId;
    private String name;
    private Integer courseId;
    private Long version;
    private AggregateState state;

    public TopicDto() {
    }

    public TopicDto(Topic topic) {
        setAggregateId(topic.getAggregateId());
        setName(topic.getName());
        setCourseId(topic.getCourseId());
        setVersion(topic.getVersion());
        setState(topic.getState());
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }
}
