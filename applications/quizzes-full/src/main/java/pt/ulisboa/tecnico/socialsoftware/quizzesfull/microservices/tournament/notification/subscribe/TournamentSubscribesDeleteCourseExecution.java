package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.Tournament;

public class TournamentSubscribesDeleteCourseExecution extends EventSubscription {

    public TournamentSubscribesDeleteCourseExecution(Tournament tournament) {
        super(tournament.getExecutionAggregateId(), tournament.getExecutionVersion(),
                DeleteCourseExecutionEvent.class.getSimpleName());
    }

    public TournamentSubscribesDeleteCourseExecution() {}
}
