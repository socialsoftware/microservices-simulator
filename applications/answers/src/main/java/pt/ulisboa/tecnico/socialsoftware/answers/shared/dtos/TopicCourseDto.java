package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicCourse;

public class TopicCourseDto implements Serializable {
    private Integer aggregateId;
    private Integer version;

    public TopicCourseDto() {
    }

    public TopicCourseDto(TopicCourse topicCourse) {
        this.aggregateId = topicCourse.getCourseAggregateId();
        this.version = topicCourse.getCourseVersion();
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
}