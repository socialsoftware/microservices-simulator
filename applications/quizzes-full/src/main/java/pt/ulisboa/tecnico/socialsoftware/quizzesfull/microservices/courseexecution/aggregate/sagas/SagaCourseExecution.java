package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecutionDto;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

@Entity
public class SagaCourseExecution extends CourseExecution implements SagaAggregate {

    @Convert(converter = SagaStateConverter.class)

    private SagaState sagaState;

    public SagaCourseExecution() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaCourseExecution(Integer aggregateId, CourseExecutionDto dto, CourseExecutionCourse courseExecutionCourse) {
        super(aggregateId, dto, courseExecutionCourse);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaCourseExecution(SagaCourseExecution other) {
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
