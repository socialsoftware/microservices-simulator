package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaQuizDto extends QuizDto {
    private SagaState sagaState;

    public SagaQuizDto(Quiz quiz) {
        super(quiz);
        this.sagaState = ((SagaQuiz)quiz).getSagaState();
    }

    public SagaState getSagaState() {
        return this.sagaState;
    }

    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}