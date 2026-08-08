package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.SagaTournament;

import java.time.LocalDateTime;

@Service
@Profile("sagas")
public class SagasTournamentFactory implements TournamentFactory {
    @Override
    public SagaTournament createTournament(Integer aggregateId, Integer executionAggregateId, Long executionVersion,
                                           Integer courseAggregateId, Integer creatorAggregateId, String creatorName,
                                           String creatorUsername, Long creatorVersion, Integer quizAggregateId,
                                           Long quizVersion, LocalDateTime startTime, LocalDateTime endTime,
                                           Integer numberOfQuestions) {
        return new SagaTournament(aggregateId, executionAggregateId, executionVersion, courseAggregateId,
                creatorAggregateId, creatorName, creatorUsername, creatorVersion, quizAggregateId, quizVersion,
                startTime, endTime, numberOfQuestions);
    }

    @Override
    public SagaTournament createTournamentCopy(Tournament existing) {
        return new SagaTournament((SagaTournament) existing);
    }

    @Override
    public TournamentDto createTournamentDto(Tournament tournament) {
        return new TournamentDto(tournament);
    }
}
