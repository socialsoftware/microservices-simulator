package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate;

import java.util.Set;

public interface TournamentCustomRepository {
    Set<Integer> findTournamentIdsByExecution(Integer executionAggregateId);
}
