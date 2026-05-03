package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Option;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionTopic;

import java.util.Set;

@Entity
public class SagaQuestion extends Question implements SagaAggregate {

    private SagaState sagaState;

    public SagaQuestion() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuestion(Integer aggregateId, String title, String content, QuestionCourse questionCourse, Set<Option> options, Set<QuestionTopic> topics) {
        super(aggregateId, title, content, questionCourse, options, topics);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuestion(SagaQuestion other) {
        super(other);
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
