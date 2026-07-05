package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.Execution;

@Entity
public class SagaExecution extends Execution implements SagaAggregate {
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private CourseExecutionSagaState sagaState;
    
    public SagaExecution() {
        super();
        this.sagaState = CourseExecutionSagaState.NOT_IN_SAGA;
    }

    public SagaExecution(Integer aggregateId, CourseExecutionDto courseExecutionDto, CourseExecutionCourse courseExecutionCourse) {
        super(aggregateId, courseExecutionDto, courseExecutionCourse);
        this.sagaState = CourseExecutionSagaState.NOT_IN_SAGA;
    }

    public SagaExecution(SagaExecution other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = (CourseExecutionSagaState) state;
    }

    @Override
    public CourseExecutionSagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public CourseExecutionSagaState getNeutralSagaState() {
        return CourseExecutionSagaState.NOT_IN_SAGA;
    }

}
