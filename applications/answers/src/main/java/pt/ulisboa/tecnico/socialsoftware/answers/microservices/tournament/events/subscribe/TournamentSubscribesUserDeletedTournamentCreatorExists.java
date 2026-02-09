package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserDeletedEvent;


public class TournamentSubscribesUserDeletedTournamentCreatorExists extends EventSubscription {
    public TournamentSubscribesUserDeletedTournamentCreatorExists(TournamentCreator creator) {
        super(creator.getCreatorAggregateId(),
                creator.getCreatorVersion(),
                UserDeletedEvent.class);
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
