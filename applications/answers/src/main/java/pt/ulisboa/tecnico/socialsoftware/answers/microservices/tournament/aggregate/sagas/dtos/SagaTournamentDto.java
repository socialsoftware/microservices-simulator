package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.sagas.SagaTournament;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaTournamentDto extends TournamentDto {
@Convert(converter = SagaStateConverter.class)
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