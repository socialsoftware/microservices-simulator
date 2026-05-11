package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizType;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
public class SagaQuiz extends Quiz implements SagaAggregate {

    private SagaState sagaState;

    public SagaQuiz() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuiz(Integer aggregateId, String title, LocalDateTime availableDate, LocalDateTime conclusionDate,
                    LocalDateTime resultsDate, QuizType quizType, QuizExecution quizExecution, Set<QuizQuestion> questions) {
        super(aggregateId, title, availableDate, conclusionDate, resultsDate, quizType, quizExecution, questions);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuiz(SagaQuiz other) {
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
