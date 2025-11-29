package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaQuiz;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaQuizDto extends QuizDto {
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