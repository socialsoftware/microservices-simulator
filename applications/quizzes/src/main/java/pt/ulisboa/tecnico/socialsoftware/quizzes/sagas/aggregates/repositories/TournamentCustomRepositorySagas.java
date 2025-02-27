package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.repositories;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository;

@Service
@Profile("sagas")
public class TournamentCustomRepositorySagas implements TournamentCustomRepository {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Override
    public Set<Integer> findAllRelevantTournamentIds(Integer executionAggregateId) {
        return tournamentRepository.findAllTournamentIdsOfNotDeletedAndNotInactiveByCourseExecutionForSaga(executionAggregateId);
    }
}