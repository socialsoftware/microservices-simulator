package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentRepository;

@Service
@Profile("sagas")
public class TournamentCustomRepositorySagas implements TournamentCustomRepository {
    @Autowired
    private TournamentRepository tournamentRepository;
}
