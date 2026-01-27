package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerUser;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.QuestionAnswered;
import java.util.List;

@Entity
public class SagaAnswer extends Answer implements SagaAggregate {
    private SagaState sagaState;

    public SagaAnswer() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaAnswer(SagaAnswer other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaAnswer(Integer aggregateId, AnswerExecution execution, AnswerUser user, AnswerQuiz quiz, AnswerDto answerDto, List<QuestionAnswered> questions) {
        super(aggregateId, execution, user, quiz, answerDto, questions);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
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