package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas.states.QuizAnswerSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.AnswerCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.AnswerStudent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.AnsweredQuiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswer;
@Entity
public class SagaQuizAnswer extends QuizAnswer implements SagaAggregate {
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private QuizAnswerSagaState sagaState;
    
    public SagaQuizAnswer() {
        super();
        this.sagaState = QuizAnswerSagaState.NOT_IN_SAGA;
    }

    public SagaQuizAnswer(Integer aggregateId, AnswerCourseExecution answerCourseExecution, AnswerStudent answerStudent, AnsweredQuiz answeredQuiz) {
        super(aggregateId, answerCourseExecution, answerStudent, answeredQuiz);
        this.sagaState = QuizAnswerSagaState.NOT_IN_SAGA;
    }

    public SagaQuizAnswer(SagaQuizAnswer other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = (QuizAnswerSagaState) state;
    }

    @Override
    public QuizAnswerSagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public QuizAnswerSagaState getNeutralSagaState() {
        return QuizAnswerSagaState.NOT_IN_SAGA;
    }
}
