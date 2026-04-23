package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.subscribe.TopicSubscribesCourseDeletedCourseRef;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Topic extends Aggregate {
    private String name;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "topic")
    private TopicCourse course;

    public Topic() {

    }

    public Topic(Integer aggregateId, TopicDto topicDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(topicDto.getName());
        setCourse(topicDto.getCourse() != null ? new TopicCourse(topicDto.getCourse()) : null);
    }


    public Topic(Topic other) {
        super(other);
        setName(other.getName());
        setCourse(other.getCourse() != null ? new TopicCourse(other.getCourse()) : null);
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
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantCourseRef(eventSubscriptions);
        }
        return eventSubscriptions;
    }
    private void interInvariantCourseRef(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new TopicSubscribesCourseDeletedCourseRef(this.getCourse()));
    }


    private boolean invariantRule0() {
        return this.name != null && this.name.length() > 0;
    }

    private boolean invariantRule1() {
        return this.course != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Topic name cannot be blank");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Topic must be associated with a course");
        }
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