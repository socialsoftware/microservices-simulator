package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.sagas.SagaAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaAnswerDto extends AnswerDto {
@Convert(converter = SagaStateConverter.class)
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