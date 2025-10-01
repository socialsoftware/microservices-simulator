package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaTournament;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaTournamentDto extends TournamentDto {
private SagaState sagaState;

public SagaTournamentDto(Tournament tournament) {
super((Tournament) tournament);
this.sagaState = ((SagaTournament)tournament).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}