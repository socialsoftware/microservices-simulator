package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;

import java.util.HashSet;
import java.util.Set;

/*
    INTRA-INVARIANTS:
        COURSE_TYPE_FINAL (final field)
        COURSE_NAME_FINAL (final field)
        CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
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

    // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
    @Column
    private int executionCount = 0;

    // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
    @Column
    private int questionCount = 0;

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
        this.executionCount = other.getExecutionCount();
        this.questionCount = other.getQuestionCount();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    @Override
    public void verifyInvariants() {
        if (executionCount == 0 && questionCount > 0) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT);
        }
    }

    public CourseType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(int executionCount) {
        this.executionCount = executionCount;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }
}
