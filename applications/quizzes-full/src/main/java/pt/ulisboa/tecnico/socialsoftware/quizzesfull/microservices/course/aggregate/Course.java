package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;

import static pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState.ACTIVE;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;

import java.util.HashSet;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.COURSE_MISSING_NAME;
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.COURSE_MISSING_TYPE;

/*
    INTRA-INVARIANTS:
        COURSE_NAME_FINAL (enforced by final field)
        COURSE_TYPE_FINAL (enforced by final field)
        COURSE_NAME_NOT_BLANK
        COURSE_TYPE_NOT_NULL
    INTER-INVARIANTS:
        (none — Course is a root publisher)
*/
@Entity
public abstract class Course extends Aggregate {

    /*
        COURSE_NAME_FINAL
    */
    @Column
    private final String name;

    /*
        COURSE_TYPE_FINAL
    */
    @Enumerated(EnumType.STRING)
    private final CourseType type;

    public Course() {
    }

    public Course(Integer aggregateId, CourseDto courseDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.name = courseDto.getName();
        this.type = courseDto.getType() != null ? CourseType.valueOf(courseDto.getType()) : null;
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

    /*
     * COURSE_NAME_NOT_BLANK
     * Course.name must not be null or blank
     */
    private boolean invariantCourseNameNotBlank() {
        return name != null && !name.isBlank();
    }

    /*
     * COURSE_TYPE_NOT_NULL
     * Course.type must not be null
     */
    private boolean invariantCourseTypeNotNull() {
        return type != null;
    }

    @Override
    public void verifyInvariants() {
        if (getState() == ACTIVE) {
            if (!invariantCourseNameNotBlank()) {
                throw new QuizzesFullException(COURSE_MISSING_NAME);
            }
            if (!invariantCourseTypeNotNull()) {
                throw new QuizzesFullException(COURSE_MISSING_TYPE);
            }
        }
    }

    public String getName() {
        return name;
    }

    public CourseType getType() {
        return type;
    }
}
