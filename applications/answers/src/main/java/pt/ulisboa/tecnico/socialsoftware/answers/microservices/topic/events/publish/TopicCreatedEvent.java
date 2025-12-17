package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TopicCreatedEvent extends Event {
    private String name;
    private TopicCourse course;

    public TopicCreatedEvent() {
    }

    public TopicCreatedEvent(Integer aggregateId, String name, TopicCourse course) {
        super(aggregateId);
        setName(name);
        setCourse(course);
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
    }

}