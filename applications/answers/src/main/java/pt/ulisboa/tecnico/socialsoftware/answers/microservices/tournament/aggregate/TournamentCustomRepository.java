package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.util.Set;

public interface TournamentCustomRepository {
    Set<Integer> findAllRelevantTournamentIds(Integer executionAggregateId);
}