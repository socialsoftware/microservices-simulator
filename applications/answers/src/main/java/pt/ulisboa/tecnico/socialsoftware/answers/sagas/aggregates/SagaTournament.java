package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates;

import jakarta.persistence.Entity;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;
import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentDto;

@Entity
public class SagaTournament extends Tournament implements SagaAggregate {
private SagaState sagaState;

public SagaTournament() {
super();
this.sagaState = GenericSagaState.NOT_IN_SAGA;
}

public SagaTournament(SagaTournament other) {
super();
this.sagaState = other.getSagaState();
}

public SagaTournament(TournamentDto tournamentDto) {
super();
this.sagaState = GenericSagaState.NOT_IN_SAGA;
// TODO: Initialize from DTO properties
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