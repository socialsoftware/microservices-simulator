package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicCourseDto;

@Entity
public class TopicCourse {
    @Id
    @GeneratedValue
    private Long id;
    private Integer courseAggregateId;
    private Integer courseVersion;
    private AggregateState courseState;
    @OneToOne
    private Topic topic;

    public TopicCourse() {

    }

    public TopicCourse(CourseDto courseDto) {
        setCourseAggregateId(courseDto.getAggregateId());
        setCourseVersion(courseDto.getVersion());
        setCourseState(courseDto.getState());
    }

    public TopicCourse(TopicCourseDto topicCourseDto) {
        setCourseAggregateId(topicCourseDto.getAggregateId());
        setCourseVersion(topicCourseDto.getVersion());
        setCourseState(topicCourseDto.getState());
    }

    public TopicCourse(TopicCourse other) {
        setCourseVersion(other.getCourseVersion());
        setCourseState(other.getCourseState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public AggregateState getCourseState() {
        return courseState;
    }

    public void setCourseState(AggregateState courseState) {
        this.courseState = courseState;
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
        dto.setState(getCourseState());
        return dto;
    }
}