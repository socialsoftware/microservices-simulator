package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentTopic;

public class TournamentSubscribesDeleteTopic extends EventSubscription {
    public TournamentSubscribesDeleteTopic(TournamentTopic topic) {
        super(topic.getTopicAggregateId(), topic.getTopicVersion(),
                DeleteTopicEvent.class.getSimpleName());
    }

    public TournamentSubscribesDeleteTopic() {
    }
}
