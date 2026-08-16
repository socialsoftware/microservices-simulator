package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentParticipant;

public class TournamentSubscribesUpdateStudentName extends EventSubscription {
    public TournamentSubscribesUpdateStudentName(TournamentCreator creator) {
        super(creator.getUserAggregateId(), creator.getUserVersion(),
                UpdateStudentNameEvent.class.getSimpleName());
    }

    public TournamentSubscribesUpdateStudentName(TournamentParticipant participant) {
        super(participant.getUserAggregateId(), participant.getUserVersion(),
                UpdateStudentNameEvent.class.getSimpleName());
    }

    public TournamentSubscribesUpdateStudentName() {
    }
}
