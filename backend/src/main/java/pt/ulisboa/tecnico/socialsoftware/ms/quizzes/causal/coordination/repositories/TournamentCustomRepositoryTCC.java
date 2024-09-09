package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.repositories;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentRepository;


@Service
@Profile("tcc")
public class TournamentCustomRepositoryTCC implements TournamentCustomRepository {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Override
    public Set<Integer> findAllRelevantTournamentIds(Integer executionAggregateId) {
        return tournamentRepository.findAllTournamentIdsOfNotDeletedAndNotInactiveByCourseExecutionForTCC(executionAggregateId);
    }
}
