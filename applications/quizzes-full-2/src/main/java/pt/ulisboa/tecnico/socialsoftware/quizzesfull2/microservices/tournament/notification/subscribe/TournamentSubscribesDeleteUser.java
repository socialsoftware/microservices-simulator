package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentParticipant;

public class TournamentSubscribesDeleteUser extends EventSubscription {
    public TournamentSubscribesDeleteUser(TournamentCreator creator) {
        super(creator.getUserAggregateId(), creator.getUserVersion(),
                DeleteUserEvent.class.getSimpleName());
    }

    public TournamentSubscribesDeleteUser(TournamentParticipant participant) {
        super(participant.getUserAggregateId(), participant.getUserVersion(),
                DeleteUserEvent.class.getSimpleName());
    }

    public TournamentSubscribesDeleteUser() {
    }
}
