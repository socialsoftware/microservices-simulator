package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate;

import java.util.List;

public interface TournamentCustomRepository {
    List<Tournament> getOpenTournamentsByExecutionId(Integer executionAggregateId);
}
