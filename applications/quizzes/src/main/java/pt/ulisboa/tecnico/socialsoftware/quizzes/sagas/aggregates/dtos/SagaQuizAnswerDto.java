package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.SagaQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaQuizAnswerDto extends QuizAnswerDto {
    private SagaState sagaState;

    public SagaQuizAnswerDto(QuizAnswer quizAnswer) {
        super(quizAnswer);
        this.sagaState = ((SagaQuizAnswer)quizAnswer).getSagaState();
    }

    public SagaState getSagaState() {
        return this.sagaState;
    }

    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}
