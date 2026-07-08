package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.sagas.SagaQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaQuestionDto extends QuestionDto {
@Convert(converter = SagaStateConverter.class)
private SagaState sagaState;

public SagaQuestionDto(Question question) {
super((Question) question);
this.sagaState = ((SagaQuestion)question).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}