package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizType;

import java.time.LocalDateTime;

@Entity
public class SagaQuiz extends Quiz implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaQuiz() {
    }

    public SagaQuiz(Integer aggregateId, Integer executionAggregateId, Long executionVersion, String title,
                    LocalDateTime creationDate, LocalDateTime availableDate, LocalDateTime conclusionDate,
                    LocalDateTime resultsDate, QuizType quizType) {
        super(aggregateId, executionAggregateId, executionVersion, title, creationDate, availableDate,
                conclusionDate, resultsDate, quizType);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuiz(SagaQuiz other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}
