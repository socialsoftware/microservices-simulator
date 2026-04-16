package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;

@Entity
public class SagaCourse extends Course implements SagaAggregate {

    private SagaState sagaState;

    public SagaCourse() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaCourse(Integer aggregateId, CourseDto courseDto) {
        super(aggregateId, courseDto);
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
