package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentCourseExecution;

public class TournamentSubscribesDeleteCourseExecution extends EventSubscription {
    public TournamentSubscribesDeleteCourseExecution(TournamentCourseExecution tournamentCourseExecution) {
        super(tournamentCourseExecution.getCourseExecutionAggregateId(),
                tournamentCourseExecution.getCourseExecutionVersion(),
                DeleteCourseExecutionEvent.class.getSimpleName());
    }

    public TournamentSubscribesDeleteCourseExecution() {}

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}