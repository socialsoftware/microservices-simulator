package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.TournamentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;

public class ExecutionDeletedEventHandler extends TournamentEventHandler {
    public ExecutionDeletedEventHandler(TournamentRepository tournamentRepository, TournamentEventProcessing tournamentEventProcessing) {
        super(tournamentRepository, tournamentEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.tournamentEventProcessing.processExecutionDeletedEvent(subscriberAggregateId, (ExecutionDeletedEvent) event);
    }
}
