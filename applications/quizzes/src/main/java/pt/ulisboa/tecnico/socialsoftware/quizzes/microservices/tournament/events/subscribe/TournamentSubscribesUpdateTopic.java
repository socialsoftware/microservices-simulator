package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentTopic;

public class TournamentSubscribesUpdateTopic extends EventSubscription {
    public TournamentSubscribesUpdateTopic(TournamentTopic tournamentTopic) {
        super(tournamentTopic.getTopicAggregateId(),
                tournamentTopic.getTopicVersion(),
                UpdateTopicEvent.class.getSimpleName());
    }

    public TournamentSubscribesUpdateTopic() {}

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}