package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicCourseDto;

@Entity
public class TopicCourse {
    @Id
    @GeneratedValue
    private Long id;
    private Integer courseAggregateId;
    private Integer courseVersion;
    @OneToOne
    private Topic topic;

    public TopicCourse() {

    }

    public TopicCourse(CourseDto courseDto) {
        setCourseAggregateId(courseDto.getAggregateId());
        setCourseVersion(courseDto.getVersion());
    }

    public TopicCourse(TopicCourse other) {
        setCourseVersion(other.getCourseVersion());
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


    public TopicCourseDto buildDto() {
        TopicCourseDto dto = new TopicCourseDto();
        dto.setAggregateId(getCourseAggregateId());
        dto.setVersion(getCourseVersion());
        return dto;
    }
}