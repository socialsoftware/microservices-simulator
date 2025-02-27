package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
@Entity
public class SagaQuestion extends Question implements SagaAggregate {
    @Column
    private SagaState sagaState;
    
    public SagaQuestion() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuestion(Integer aggregateId, QuestionCourse questionCourse, QuestionDto questionDto, List<QuestionTopic> questionTopics) {
        super(aggregateId, questionCourse, questionDto, questionTopics);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuestion(SagaQuestion other) {
        super(other);
        this.sagaState = other.getSagaState();
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
