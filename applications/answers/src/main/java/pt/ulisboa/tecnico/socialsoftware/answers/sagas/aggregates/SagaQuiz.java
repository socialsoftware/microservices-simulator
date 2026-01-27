package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizQuestion;
import java.util.Set;

@Entity
public class SagaQuiz extends Quiz implements SagaAggregate {
    private SagaState sagaState;

    public SagaQuiz() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuiz(SagaQuiz other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaQuiz(Integer aggregateId, QuizExecution execution, QuizDto quizDto, Set<QuizQuestion> questions) {
        super(aggregateId, execution, quizDto, questions);
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