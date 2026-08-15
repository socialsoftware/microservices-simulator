package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
public abstract class Course extends Aggregate {
    private final String name;
    @Enumerated(EnumType.STRING)
    private final CourseType type;

    public Course() {
        this.name = null;
        this.type = null;
    }

    public Course(Integer aggregateId, String name, CourseType type) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.name = name;
        this.type = type;
    }

    public Course(Course other) {
        super(other);
        this.name = other.getName();
        this.type = other.getType();
    }

    @Override
    public void verifyInvariants() {
        // COURSE_NAME_FINAL and COURSE_TYPE_FINAL are enforced by the `final` fields above;
        // Course has no other P1 rule.
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public CourseType getType() {
        return type;
    }
}
