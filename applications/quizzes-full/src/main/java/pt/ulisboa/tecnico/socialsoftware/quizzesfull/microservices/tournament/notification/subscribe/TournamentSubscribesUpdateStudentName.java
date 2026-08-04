package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent;

public class TournamentSubscribesUpdateStudentName extends EventSubscription {

    public TournamentSubscribesUpdateStudentName(Integer userAggregateId, Long userVersion) {
        super(userAggregateId, userVersion, UpdateStudentNameEvent.class.getSimpleName());
    }

    public TournamentSubscribesUpdateStudentName() {}
}
