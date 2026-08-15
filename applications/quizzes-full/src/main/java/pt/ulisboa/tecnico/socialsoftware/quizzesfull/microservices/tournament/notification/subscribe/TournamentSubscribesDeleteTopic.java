package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentTopic;

public class TournamentSubscribesDeleteTopic extends EventSubscription {

    public TournamentSubscribesDeleteTopic(TournamentTopic topic) {
        super(topic.getTopicAggregateId(), topic.getTopicVersion(), DeleteTopicEvent.class.getSimpleName());
    }

    public TournamentSubscribesDeleteTopic() {}
}
