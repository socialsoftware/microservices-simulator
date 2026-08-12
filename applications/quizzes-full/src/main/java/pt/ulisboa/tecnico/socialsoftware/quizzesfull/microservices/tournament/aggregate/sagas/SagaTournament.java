package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentTopic;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
public class SagaTournament extends Tournament implements SagaAggregate {

    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaTournament() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTournament(Integer aggregateId,
                          Integer executionAggregateId, Long executionVersion,
                          Integer creatorAggregateId, String creatorName, String creatorUsername, Long creatorVersion,
                          Integer quizAggregateId, Long quizVersion,
                          Set<TournamentTopic> topics,
                          LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions) {
        super(aggregateId, executionAggregateId, executionVersion,
                creatorAggregateId, creatorName, creatorUsername, creatorVersion,
                quizAggregateId, quizVersion, topics, startTime, endTime, numberOfQuestions);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTournament(SagaTournament other) {
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
