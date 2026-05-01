package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicCourse;

@Entity
public class SagaTopic extends Topic implements SagaAggregate {

    private SagaState sagaState;

    public SagaTopic() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTopic(Integer aggregateId, String name, TopicCourse topicCourse) {
        super(aggregateId, name, topicCourse);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTopic(SagaTopic other) {
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
