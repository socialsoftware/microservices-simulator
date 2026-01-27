package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaTournament;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;
import java.util.Set;

@Service
@Profile("sagas")
public class SagasTournamentFactory implements TournamentFactory {
    @Override
    public Tournament createTournament(Integer aggregateId, TournamentCreator creator, TournamentExecution execution, TournamentQuiz quiz, TournamentDto tournamentDto, Set<TournamentParticipant> participants, Set<TournamentTopic> topics) {
        return new SagaTournament(aggregateId, creator, execution, quiz, tournamentDto, participants, topics);
    }

    @Override
    public Tournament createTournamentFromExisting(Tournament existingTournament) {
        return new SagaTournament((SagaTournament) existingTournament);
    }

    @Override
    public TournamentDto createTournamentDto(Tournament tournament) {
        return new SagaTournamentDto(tournament);
    }
}