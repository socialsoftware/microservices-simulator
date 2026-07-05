package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.sagas;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.sagas.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionTopic;

import java.util.List;
@Entity
public class SagaQuestion extends Question implements SagaAggregate {
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private QuestionSagaState sagaState;
    
    public SagaQuestion() {
        super();
        this.sagaState = QuestionSagaState.NOT_IN_SAGA;
    }

    public SagaQuestion(Integer aggregateId, QuestionCourse questionCourse, QuestionDto questionDto, List<QuestionTopic> questionTopics) {
        super(aggregateId, questionCourse, questionDto, questionTopics);
        this.sagaState = QuestionSagaState.NOT_IN_SAGA;
    }

    public SagaQuestion(SagaQuestion other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = (QuestionSagaState) state;
    }

    @Override
    public QuestionSagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public QuestionSagaState getNeutralSagaState() {
        return QuestionSagaState.NOT_IN_SAGA;
    }
    
}
