package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates;

import jakarta.persistence.Entity;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;
import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionDto;

@Entity
public class SagaQuestion extends Question implements SagaAggregate {
private SagaState sagaState;

public SagaQuestion() {
super();
this.sagaState = GenericSagaState.NOT_IN_SAGA;
}

public SagaQuestion(SagaQuestion other) {
super();
this.sagaState = other.getSagaState();
}

public SagaQuestion(QuestionDto questionDto) {
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