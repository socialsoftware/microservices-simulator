package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserDeletedEvent;


public class TournamentSubscribesUserDeletedTournamentParticipantsExist extends EventSubscription {
    public TournamentSubscribesUserDeletedTournamentParticipantsExist(TournamentParticipant participants) {
        super(participants.getParticipantAggregateId(),
                participants.getParticipantVersion(),
                UserDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
