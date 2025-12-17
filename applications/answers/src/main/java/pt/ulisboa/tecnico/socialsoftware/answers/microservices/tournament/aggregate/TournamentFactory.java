package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;

public interface TournamentFactory {
    Tournament createTournament(Integer aggregateId, TournamentCreator creator, TournamentExecution execution, TournamentQuiz quiz, TournamentDto tournamentDto, Set<TournamentParticipant> participants, Set<TournamentTopic> topics);
    Tournament createTournamentFromExisting(Tournament existingTournament);
    TournamentDto createTournamentDto(Tournament tournament);
}
