package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Option;
import java.util.Set;
import java.util.List;

@Entity
public class SagaQuestion extends Question implements SagaAggregate {
    private SagaState sagaState;

    public SagaQuestion() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuestion(SagaQuestion other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaQuestion(Integer aggregateId, QuestionCourse course, QuestionDto questionDto, Set<QuestionTopic> topics, List<Option> options) {
        super(aggregateId, course, questionDto, topics, options);
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