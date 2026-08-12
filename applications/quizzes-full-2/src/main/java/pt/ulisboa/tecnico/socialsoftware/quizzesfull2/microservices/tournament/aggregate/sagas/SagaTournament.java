package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.Tournament;

import java.time.LocalDateTime;

@Entity
public class SagaTournament extends Tournament implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaTournament() {
    }

    public SagaTournament(Integer aggregateId, Integer executionAggregateId, Long executionVersion,
                          Integer courseAggregateId, Integer creatorAggregateId, String creatorName,
                          String creatorUsername, Long creatorVersion, Integer quizAggregateId, Long quizVersion,
                          LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions) {
        super(aggregateId, executionAggregateId, executionVersion, courseAggregateId, creatorAggregateId,
                creatorName, creatorUsername, creatorVersion, quizAggregateId, quizVersion, startTime, endTime,
                numberOfQuestions);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTournament(SagaTournament other) {
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
