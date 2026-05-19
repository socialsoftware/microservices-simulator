package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent;

public class TournamentSubscribesDeleteUser extends EventSubscription {

    public TournamentSubscribesDeleteUser(Integer userAggregateId, Long userVersion) {
        super(userAggregateId, userVersion, DeleteUserEvent.class.getSimpleName());
    }

    public TournamentSubscribesDeleteUser() {}
}
