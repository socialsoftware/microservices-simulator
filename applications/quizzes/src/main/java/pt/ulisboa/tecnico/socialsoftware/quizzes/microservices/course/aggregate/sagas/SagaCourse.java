package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
@Entity
public class SagaCourse extends Course implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;
    
    public SagaCourse() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaCourse(Integer aggregateId, CourseExecutionDto courseExecutionDto) {
        super(aggregateId, courseExecutionDto);
        this.sagaState = GenericSagaState.NOT_IN_SAGA; 
    }

    public SagaCourse(SagaCourse other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }
}
