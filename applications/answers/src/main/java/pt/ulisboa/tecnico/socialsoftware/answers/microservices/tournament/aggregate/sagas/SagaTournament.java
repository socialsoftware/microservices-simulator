package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;

@Entity
public class SagaTournament extends Tournament implements SagaAggregate {
    @jakarta.persistence.Convert(converter = pt.ulisboa.tecnico.socialsoftware.answers.shared.sagaStates.SagaStateConverter.class)
    private SagaState sagaState;

    public SagaTournament() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTournament(SagaTournament other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaTournament(Integer aggregateId, TournamentDto tournamentDto) {
        super(aggregateId, tournamentDto);
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