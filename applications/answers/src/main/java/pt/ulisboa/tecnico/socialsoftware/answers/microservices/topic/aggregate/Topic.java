package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;

@Entity
public abstract class Topic extends Aggregate {
    private String name;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "topic")
    private TopicCourse course;

    public Topic() {

    }

    public Topic(Integer aggregateId, TopicCourse course, TopicDto topicDto) {
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

    public TopicDto buildDto() {
        TopicDto dto = new TopicDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setCourse(getCourse() != null ? new TopicCourseDto(getCourse()) : null);
        return dto;
    }
}