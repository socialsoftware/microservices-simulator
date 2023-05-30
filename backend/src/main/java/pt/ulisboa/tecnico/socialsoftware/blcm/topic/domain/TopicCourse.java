package pt.ulisboa.tecnico.socialsoftware.blcm.topic.domain;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.blcm.course.domain.Course;
import pt.ulisboa.tecnico.socialsoftware.blcm.course.dto.CourseDto;

@Entity
public class TopicCourse {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer courseAggregateId;
    private Integer courseVersion;
    @OneToOne
    private Topic topic;

    public TopicCourse() {}

    public TopicCourse(CourseDto courseDto) {
        setCourseAggregateId(courseDto.getAggregateId());
        setCourseVersion(courseDto.getVersion());
    }

    public TopicCourse(TopicCourse other) {
        setCourseAggregateId(other.getCourseAggregateId());
        setCourseVersion(other.getCourseVersion());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getCourseVersion() {
        return courseVersion;
    }

    public void setCourseVersion(Integer courseVersion) {
        this.courseVersion = courseVersion;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }
}
