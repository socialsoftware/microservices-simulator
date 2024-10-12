package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.eventProcessing.TournamentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.events.publish.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentRepository;

public class InvalidateQuizEventHandler extends TournamentEventHandler {
    public InvalidateQuizEventHandler(TournamentRepository tournamentRepository, TournamentEventProcessing tournamentEventProcessing) {
        super(tournamentRepository, tournamentEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.tournamentEventProcessing.processInvalidateQuizEvent(subscriberAggregateId, (InvalidateQuizEvent) event);
    }
}
