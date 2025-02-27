package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.SagaQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaQuestionDto extends QuestionDto {
    private SagaState sagaState;

    public SagaQuestionDto(Question question) {
        super(question);
        this.sagaState = ((SagaQuestion)question).getSagaState();
    }

    public SagaState getSagaState() {
        return this.sagaState;
    }

    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}
