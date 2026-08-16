package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentExecution;

public class TournamentSubscribesDeleteCourseExecution extends EventSubscription {
    public TournamentSubscribesDeleteCourseExecution(TournamentExecution execution) {
        super(execution.getExecutionAggregateId(), execution.getExecutionVersion(),
                DeleteCourseExecutionEvent.class.getSimpleName());
    }

    public TournamentSubscribesDeleteCourseExecution() {
    }
}
