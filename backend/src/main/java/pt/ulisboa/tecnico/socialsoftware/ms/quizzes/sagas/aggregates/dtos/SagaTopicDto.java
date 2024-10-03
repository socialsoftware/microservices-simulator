package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaTopicDto extends TopicDto {
    private SagaState sagaState;

    public SagaTopicDto(Topic topic) {
        super(topic);
        this.sagaState = ((SagaTopic)topic).getSagaState();
    }

    public SagaState getSagaState() {
        return this.sagaState;
    }

    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}