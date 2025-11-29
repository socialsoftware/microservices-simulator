package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaAnswer;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaAnswerDto extends AnswerDto {
private SagaState sagaState;

public SagaAnswerDto(Answer answer) {
super((Answer) answer);
this.sagaState = ((SagaAnswer)answer).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}