package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;

public interface TournamentFactory {
    Tournament createTournament(Integer aggregateId, TournamentDto tournamentDto);
    Tournament createTournamentFromExisting(Tournament existingTournament);
    TournamentDto createTournamentDto(Tournament tournament);
}
