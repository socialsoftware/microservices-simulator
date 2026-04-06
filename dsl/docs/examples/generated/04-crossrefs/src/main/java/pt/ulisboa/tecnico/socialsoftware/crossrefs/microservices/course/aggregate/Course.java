package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.events.subscribe.CourseSubscribesTeacherDeletedTeacherExists;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseTeacherDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Course extends Aggregate {
    private String title;
    private String description;
    private Integer maxStudents;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "course")
    private CourseTeacher teacher;

    public Course() {

    }

    public Course(Integer aggregateId, CourseDto courseDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTitle(courseDto.getTitle());
        setDescription(courseDto.getDescription());
        setMaxStudents(courseDto.getMaxStudents());
        setTeacher(courseDto.getTeacher() != null ? new CourseTeacher(courseDto.getTeacher()) : null);
    }


    public Course(Course other) {
        super(other);
        setTitle(other.getTitle());
        setDescription(other.getDescription());
        setMaxStudents(other.getMaxStudents());
        setTeacher(new CourseTeacher(other.getTeacher()));
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }

    public CourseTeacher getTeacher() {
        return teacher;
    }

    public void setTeacher(CourseTeacher teacher) {
        this.teacher = teacher;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantTeacherExists(eventSubscriptions);
        }
        return eventSubscriptions;
    }
    private void interInvariantTeacherExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new CourseSubscribesTeacherDeletedTeacherExists(this.getTeacher()));
    }


    private boolean invariantTitleNotBlank() {
        return this.title != null && this.title.length() > 0;
    }

    private boolean invariantMaxStudentsPositive() {
        return maxStudents > 0;
    }

    private boolean invariantTeacherNotNull() {
        return this.teacher != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantTitleNotBlank()) {
            throw new SimulatorException(INVARIANT_BREAK, "Course title cannot be blank");
        }
        if (!invariantMaxStudentsPositive()) {
            throw new SimulatorException(INVARIANT_BREAK, "Max students must be positive");
        }
        if (!invariantTeacherNotNull()) {
            throw new SimulatorException(INVARIANT_BREAK, "Course must have a teacher");
        }
    }

    public CourseDto buildDto() {
        CourseDto dto = new CourseDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setTitle(getTitle());
        dto.setDescription(getDescription());
        dto.setMaxStudents(getMaxStudents());
        dto.setTeacher(getTeacher() != null ? new CourseTeacherDto(getTeacher()) : null);
        return dto;
    }
}