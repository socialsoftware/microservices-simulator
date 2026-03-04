package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionDeletedEvent;

public class TournamentSubscribesExecutionDeleted extends EventSubscription {
    public TournamentSubscribesExecutionDeleted(Tournament tournament) {
        super(tournament.getAggregateId(), 0, ExecutionDeletedEvent.class.getSimpleName());
    }
}
