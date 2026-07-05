package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.*;

import java.util.Set;
@Entity
public class SagaQuiz extends Quiz implements SagaAggregate {
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private QuizSagaState sagaState;
    
    public SagaQuiz() {
        this.sagaState = QuizSagaState.NOT_IN_SAGA;
    }

    public SagaQuiz(Integer aggregateId, QuizCourseExecution quizCourseExecution, Set<QuizQuestion> quizQuestions, QuizDto quizDto, QuizType quizType) {
        super(aggregateId, quizCourseExecution, quizQuestions, quizDto, quizType);
        this.sagaState = QuizSagaState.NOT_IN_SAGA;
    }

    public SagaQuiz(SagaQuiz other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = (QuizSagaState) state;
    }

    @Override
    public QuizSagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public QuizSagaState getNeutralSagaState() {
        return QuizSagaState.NOT_IN_SAGA;
    }
}
