package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.ExecutionDeletedEventHandler;

public class TournamentSubscribesExecutionDeleted extends EventSubscription {
    public TournamentSubscribesExecutionDeleted(Tournament tournament) {
        super(tournament,
                ExecutionDeletedEvent.class,
                ExecutionDeletedEventHandler.class);
    }
}
