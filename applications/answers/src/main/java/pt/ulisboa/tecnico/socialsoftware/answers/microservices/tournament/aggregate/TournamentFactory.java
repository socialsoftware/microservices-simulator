package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

public interface TournamentFactory {
    Tournament createTournament(Integer aggregateId,  Dto);
    Tournament createTournamentFromExisting(Tournament existingTournament);
     createTournamentDto(Tournament );
}
