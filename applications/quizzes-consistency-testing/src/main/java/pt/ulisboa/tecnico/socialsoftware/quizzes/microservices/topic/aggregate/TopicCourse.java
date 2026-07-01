package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;

@Entity
public class TopicCourse {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer courseAggregateId;
    private Long courseVersion;
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

    public Long getCourseVersion() {
        return courseVersion;
    }

    public void setCourseVersion(Long courseVersion) {
        this.courseVersion = courseVersion;
    }

    @JsonIgnore
    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }
}
