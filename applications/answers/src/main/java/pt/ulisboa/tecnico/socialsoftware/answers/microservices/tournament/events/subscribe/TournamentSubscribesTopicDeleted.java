package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TopicDeletedEvent;

public class TournamentSubscribesTopicDeleted extends EventSubscription {
    

    public TournamentSubscribesTopicDeleted( ) {
        super(.getCreator().getCreatorAggregateId(),
                .getCreator().getCreatorVersion(),
                TopicDeletedEvent.class.getSimpleName());
        
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
