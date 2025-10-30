package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository;

public class AnonymizeStudentEventHandler extends TournamentEventHandler {
    public AnonymizeStudentEventHandler(TournamentRepository tournamentRepository, TournamentEventProcessing tournamentEventProcessing) {
        super(tournamentRepository, tournamentEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.tournamentEventProcessing.processAnonymizeStudentEvent(subscriberAggregateId, (AnonymizeStudentEvent) event);
    }
}
