package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
@Entity
public class SagaCourse extends Course implements SagaAggregate {
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private CourseSagaState sagaState;
    
    public SagaCourse() {
        super();
        this.sagaState = CourseSagaState.NOT_IN_SAGA;
    }

    public SagaCourse(Integer aggregateId, CourseExecutionDto courseExecutionDto) {
        super(aggregateId, courseExecutionDto);
        this.sagaState = CourseSagaState.NOT_IN_SAGA; 
    }

    public SagaCourse(SagaCourse other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = (CourseSagaState) state;
    }

    @Override
    public CourseSagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public CourseSagaState getNeutralSagaState() {
        return CourseSagaState.NOT_IN_SAGA;
    }
}
