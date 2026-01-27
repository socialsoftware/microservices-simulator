package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

import java.util.HashSet;
import java.util.Set;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType;

@Entity
public abstract class Course extends Aggregate {
    private String name;
    @Enumerated(EnumType.STRING)
    private final CourseType type;
    private LocalDateTime creationDate;

    public Course() {
        this.type = CourseType.TECNICO;
    }

    public Course(Integer aggregateId, CourseDto courseDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(courseDto.getName());
        this.type = courseDto.getType() != null ? CourseType.valueOf(courseDto.getType()) : CourseType.TECNICO;
        setCreationDate(courseDto.getCreationDate());
    }

    public Course(Course other) {
        super(other);
        setName(other.getName());
        this.type = other.getType();
        setCreationDate(other.getCreationDate());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CourseType getType() {
        return type;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    @Override
    public void verifyInvariants() {
        // No invariants defined
    }

    public CourseDto buildDto() {
        CourseDto dto = new CourseDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setType(getType() != null ? getType().name() : null);
        dto.setCreationDate(getCreationDate());
        return dto;
    }
}