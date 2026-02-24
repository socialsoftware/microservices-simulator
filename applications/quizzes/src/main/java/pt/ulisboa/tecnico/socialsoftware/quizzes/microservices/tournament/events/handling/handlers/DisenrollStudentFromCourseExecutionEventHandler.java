package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing;

public class DisenrollStudentFromCourseExecutionEventHandler extends TournamentEventHandler {
    public DisenrollStudentFromCourseExecutionEventHandler(TournamentRepository tournamentRepository, TournamentEventProcessing tournamentEventProcessing) {
        super(tournamentRepository, tournamentEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.tournamentEventProcessing.processDisenrollStudentFromCourseExecutionEvent(subscriberAggregateId, (DisenrollStudentFromCourseExecutionEvent) event);
    }
}
