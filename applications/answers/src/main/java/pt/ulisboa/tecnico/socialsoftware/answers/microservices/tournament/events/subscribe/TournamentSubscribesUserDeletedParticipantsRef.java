package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.answers.events.UserDeletedEvent;


public class TournamentSubscribesUserDeletedParticipantsRef extends EventSubscription {
    public TournamentSubscribesUserDeletedParticipantsRef(TournamentParticipant participants) {
        super(participants.getParticipantAggregateId(),
                participants.getParticipantVersion(),
                UserDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
