package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.Course;

@Entity
public class SagaCourse extends Course implements SagaAggregate {

    private SagaState sagaState;

    public SagaCourse() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaCourse(Integer aggregateId, String name, String type) {
        super(aggregateId, name, type);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaCourse(SagaCourse other) {
        super(other);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
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
