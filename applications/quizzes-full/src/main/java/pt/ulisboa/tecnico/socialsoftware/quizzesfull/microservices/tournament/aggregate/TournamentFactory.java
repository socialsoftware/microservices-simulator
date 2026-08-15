package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate;

import java.time.LocalDateTime;
import java.util.Set;

public interface TournamentFactory {
    Tournament createTournament(Integer aggregateId,
                                Integer executionAggregateId, Long executionVersion,
                                Integer creatorAggregateId, String creatorName, String creatorUsername, Long creatorVersion,
                                Integer quizAggregateId, Long quizVersion,
                                Set<TournamentTopic> topics,
                                LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions);
    Tournament createTournamentCopy(Tournament existing);
    TournamentDto createTournamentDto(Tournament tournament);
}
