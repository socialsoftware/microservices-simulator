package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;

import java.util.HashSet;
import java.util.Set;

/*
    INTRA-INVARIANTS:
        COURSE_TYPE_FINAL (final field)
        COURSE_NAME_FINAL (final field)
    INTER-INVARIANTS:
        (none)
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
        this.name = null;
        this.type = null;
    }

    public Course(Integer aggregateId, String name, String type) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.name = name;
        this.type = CourseType.valueOf(type);
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
    public void verifyInvariants() {}

    public CourseType getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
