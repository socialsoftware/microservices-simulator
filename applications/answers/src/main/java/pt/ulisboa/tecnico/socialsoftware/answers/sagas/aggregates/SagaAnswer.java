package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates;

import jakarta.persistence.Entity;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;
import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerDto;

@Entity
public class SagaAnswer extends Answer implements SagaAggregate {
private SagaState sagaState;

public SagaAnswer() {
super();
this.sagaState = GenericSagaState.NOT_IN_SAGA;
}

public SagaAnswer(SagaAnswer other) {
super();
this.sagaState = other.getSagaState();
}

public SagaAnswer(AnswerDto answerDto) {
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