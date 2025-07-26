package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.SagaTournament;

public class SagaTournamentDto extends TournamentDto {
    private SagaState sagaState;

    public SagaTournamentDto(Tournament tournament) {
        super(tournament);
        this.sagaState = ((SagaTournament)tournament).getSagaState();
    }

    public SagaState getSagaState() {
        return this.sagaState;
    }

    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}
