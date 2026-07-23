package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentTopic;

public class TournamentSubscribesUpdateTopic extends EventSubscription {

    public TournamentSubscribesUpdateTopic(TournamentTopic topic) {
        super(topic.getTopicAggregateId(), topic.getTopicVersion(), UpdateTopicEvent.class.getSimpleName());
    }

    public TournamentSubscribesUpdateTopic() {}
}
