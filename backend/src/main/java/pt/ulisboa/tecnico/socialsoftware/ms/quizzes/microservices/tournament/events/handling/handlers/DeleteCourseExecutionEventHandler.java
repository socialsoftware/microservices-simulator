package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.eventProcessing.TournamentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentRepository;

public class DeleteCourseExecutionEventHandler extends TournamentEventHandler {
    public DeleteCourseExecutionEventHandler(TournamentRepository tournamentRepository, TournamentEventProcessing tournamentEventProcessing) {
        super(tournamentRepository, tournamentEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.tournamentEventProcessing.processRemoveCourseExecutionEvent(subscriberAggregateId, (DeleteCourseExecutionEvent) event);
    }
}
