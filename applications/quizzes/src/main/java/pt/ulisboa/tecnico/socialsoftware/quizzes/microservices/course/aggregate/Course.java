package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

import java.util.HashSet;
import java.util.Set;

/*
    INTRA-INVARIANTS:
        COURSE_TYPE_FINAL
        COURSE_NAME_FINAL
    INTER_INVARIANTS:
 */
@Entity
public abstract class Course extends Aggregate {
    /*
        COURSE_TYPE_FINAL
     */
    @Enumerated(EnumType.STRING)
    private final CourseType type;
    /*
        COURSE_NAME_FINAL
     */
    @Column
    private final String name;

    public Course() {
        this.name = "COURSE NAME";
        this.type = CourseType.TECNICO;
    }

    public Course(Integer aggregateId, CourseExecutionDto courseExecutionDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.name = courseExecutionDto.getName();
        this.type = CourseType.valueOf(courseExecutionDto.getType());
    }

    public Course(Course other) {
        super(other);
        this.name = other.getName();
        this.type = other.getType();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    @Override
    public void verifyInvariants() {

    }
    public CourseType getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
