package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("sagas")
public class TournamentCustomRepositorySagas implements TournamentCustomRepository {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Override
    public List<Tournament> getOpenTournamentsByExecutionId(Integer executionAggregateId) {
        return tournamentRepository.findAllLatestActive().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getCancelled()))
                .filter(t -> executionAggregateId.equals(t.getExecutionAggregateId()))
                .filter(t -> t.getEndTime() != null && t.getEndTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
    }
}
