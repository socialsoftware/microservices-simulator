package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.sagas.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaTopicDto extends TopicDto {
@Convert(converter = SagaStateConverter.class)
private SagaState sagaState;

public SagaTopicDto(Topic topic) {
super((Topic) topic);
this.sagaState = ((SagaTopic)topic).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}