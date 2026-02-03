package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TopicUpdatedEvent;

public class TournamentSubscribesTopicUpdated extends EventSubscription {
    

    public TournamentSubscribesTopicUpdated( ) {
        super(.getCreator().getCreatorAggregateId(),
                .getCreator().getCreatorVersion(),
                TopicUpdatedEvent.class.getSimpleName());
        
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
