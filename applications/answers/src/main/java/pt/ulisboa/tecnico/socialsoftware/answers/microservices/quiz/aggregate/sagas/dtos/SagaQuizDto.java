package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.sagas.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaQuizDto extends QuizDto {
@Convert(converter = SagaStateConverter.class)
private SagaState sagaState;

public SagaQuizDto(Quiz quiz) {
super((Quiz) quiz);
this.sagaState = ((SagaQuiz)quiz).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}