package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaTournament;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTournamentDto;

@Service
@Profile("sagas")
public class SagasTournamentFactory extends TournamentFactory {
@Override
public Tournament createTournament(Integer aggregateId, TournamentDto tournamentDto) {
return new SagaTournament(tournamentDto);
}

@Override
public Tournament createTournamentFromExisting(Tournament existingTournament) {
return new SagaTournament((SagaTournament) existingTournament);
}

@Override
public TournamentDto createTournamentDto(Tournament tournament) {
return new SagaTournamentDto(tournament);
}
}