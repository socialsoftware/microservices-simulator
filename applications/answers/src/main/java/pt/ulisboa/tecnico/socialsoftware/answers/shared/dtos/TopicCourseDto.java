package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicCourse;

public class TopicCourseDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public TopicCourseDto() {
    }

    public TopicCourseDto(TopicCourse topicCourse) {
        this.aggregateId = topicCourse.getCourseAggregateId();
        this.version = topicCourse.getCourseVersion();
        this.state = topicCourse.getCourseState();
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
}