package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;
import java.util.Set;

@Entity
public class SagaTournament extends Tournament implements SagaAggregate {
    private SagaState sagaState;

    public SagaTournament() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTournament(SagaTournament other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaTournament(Integer aggregateId, TournamentCreator creator, TournamentExecution execution, TournamentQuiz quiz, TournamentDto tournamentDto, Set<TournamentParticipant> participants, Set<TournamentTopic> topics) {
        super(aggregateId, creator, execution, quiz, tournamentDto, participants, topics);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }
}