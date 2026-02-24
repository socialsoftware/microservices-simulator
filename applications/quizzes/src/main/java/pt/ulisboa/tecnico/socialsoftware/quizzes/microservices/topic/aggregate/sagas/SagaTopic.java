package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicCourse;
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
