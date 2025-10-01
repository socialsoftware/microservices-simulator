package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates;

import jakarta.persistence.Entity;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;
import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicDto;

@Entity
public class SagaTopic extends Topic implements SagaAggregate {
private SagaState sagaState;

public SagaTopic() {
super();
this.sagaState = GenericSagaState.NOT_IN_SAGA;
}

public SagaTopic(SagaTopic other) {
super();
this.sagaState = other.getSagaState();
}

public SagaTopic(TopicDto topicDto) {
super();
this.sagaState = GenericSagaState.NOT_IN_SAGA;
// TODO: Initialize from DTO properties
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