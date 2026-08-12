package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate;

import java.util.Optional;
import java.util.Set;

public interface TournamentCustomRepository {
    Set<Integer> findAllRelevantTournamentIds(Integer executionAggregateId);
    Optional<Tournament> findTournamentById(Integer aggregateId);
}