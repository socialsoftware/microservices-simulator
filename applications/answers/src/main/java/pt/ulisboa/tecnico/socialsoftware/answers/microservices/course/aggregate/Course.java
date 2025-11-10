package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType;

@Entity
public abstract class Course extends Aggregate {
    private String name;
    @Enumerated(EnumType.STRING)
    private CourseType type;
    private LocalDateTime creationDate;

    public Course() {

    }

    public Course(Integer aggregateId, CourseDto courseDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(courseDto.getName());
        setType(CourseType.valueOf(courseDto.getType()));
        setCreationDate(courseDto.getCreationDate());
    }

    public Course(Course other) {
        super(other);
        setName(other.getName());
        setType(other.getType());
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

    public void setType(CourseType type) {
        this.type = type;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }


}