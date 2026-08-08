package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate;

import java.time.LocalDateTime;

public interface TournamentFactory {
    Tournament createTournament(Integer aggregateId, Integer executionAggregateId, Long executionVersion,
                                Integer courseAggregateId, Integer creatorAggregateId, String creatorName,
                                String creatorUsername, Long creatorVersion, Integer quizAggregateId,
                                Long quizVersion, LocalDateTime startTime, LocalDateTime endTime,
                                Integer numberOfQuestions);

    Tournament createTournamentCopy(Tournament existing);

    TournamentDto createTournamentDto(Tournament tournament);
}
