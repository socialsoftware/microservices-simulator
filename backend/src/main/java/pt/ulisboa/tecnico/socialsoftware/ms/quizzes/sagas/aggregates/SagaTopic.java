package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicCourse;


@Entity
public class SagaTopic extends Topic implements SagaAggregate {
    @OneToOne
    private SagaState sagaState;
    
    public SagaTopic() {
        super();
    }

    public SagaTopic(Integer aggregateId, String name, TopicCourse topicCourse) {
        super(aggregateId, name, topicCourse);
    }

    public SagaTopic(SagaTopic other) {
        super(other);
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
