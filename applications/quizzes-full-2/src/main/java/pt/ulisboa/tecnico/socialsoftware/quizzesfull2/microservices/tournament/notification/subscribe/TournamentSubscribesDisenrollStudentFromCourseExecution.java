package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentExecution;

public class TournamentSubscribesDisenrollStudentFromCourseExecution extends EventSubscription {
    public TournamentSubscribesDisenrollStudentFromCourseExecution(TournamentExecution execution) {
        super(execution.getExecutionAggregateId(), execution.getExecutionVersion(),
                DisenrollStudentFromCourseExecutionEvent.class.getSimpleName());
    }

    public TournamentSubscribesDisenrollStudentFromCourseExecution() {
    }
}
