package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicDeletedEvent;


public class TournamentSubscribesTopicDeletedTournamentTopicsExist extends EventSubscription {
    public TournamentSubscribesTopicDeletedTournamentTopicsExist(TournamentTopic topics) {
        super(topics.getTopicAggregateId(),
                topics.getTopicVersion(),
                TopicDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
