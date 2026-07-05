package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.sagas;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.sagas.states.TopicSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicCourse;
@Entity
public class SagaTopic extends Topic implements SagaAggregate {
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private TopicSagaState sagaState;
    
    public SagaTopic() {
        super();
        this.sagaState = TopicSagaState.NOT_IN_SAGA;
    }

    public SagaTopic(Integer aggregateId, String name, TopicCourse topicCourse) {
        super(aggregateId, name, topicCourse);
        this.sagaState = TopicSagaState.NOT_IN_SAGA;
    }

    public SagaTopic(SagaTopic other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = (TopicSagaState) state;
    }

    @Override
    public TopicSagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public TopicSagaState getNeutralSagaState() {
        return TopicSagaState.NOT_IN_SAGA;
    }
}
