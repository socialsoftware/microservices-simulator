package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentParticipant;

public class TournamentSubscribesAnonymizeStudent extends EventSubscription {
    public TournamentSubscribesAnonymizeStudent(TournamentCreator creator) {
        super(creator.getUserAggregateId(), creator.getUserVersion(),
                AnonymizeStudentEvent.class.getSimpleName());
    }

    public TournamentSubscribesAnonymizeStudent(TournamentParticipant participant) {
        super(participant.getUserAggregateId(), participant.getUserVersion(),
                AnonymizeStudentEvent.class.getSimpleName());
    }

    public TournamentSubscribesAnonymizeStudent() {
    }
}
