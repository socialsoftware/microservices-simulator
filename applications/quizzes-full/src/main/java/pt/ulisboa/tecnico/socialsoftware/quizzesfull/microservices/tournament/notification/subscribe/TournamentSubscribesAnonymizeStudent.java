package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent;

public class TournamentSubscribesAnonymizeStudent extends EventSubscription {

    public TournamentSubscribesAnonymizeStudent(Integer userAggregateId, Long userVersion) {
        super(userAggregateId, userVersion, AnonymizeStudentEvent.class.getSimpleName());
    }

    public TournamentSubscribesAnonymizeStudent() {}
}
