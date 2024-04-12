package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.*;

@Entity
public class SagaQuizAnswer extends QuizAnswer implements SagaAggregate {
    @OneToOne
    private SagaState sagaState;
    
    public SagaQuizAnswer() {
        super();
    }

    public SagaQuizAnswer(Integer aggregateId, AnswerCourseExecution answerCourseExecution, AnswerStudent answerStudent, AnsweredQuiz answeredQuiz) {
        super(aggregateId, answerCourseExecution, answerStudent, answeredQuiz);
    }

    public SagaQuizAnswer(SagaQuizAnswer other) {
        super(other);
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
