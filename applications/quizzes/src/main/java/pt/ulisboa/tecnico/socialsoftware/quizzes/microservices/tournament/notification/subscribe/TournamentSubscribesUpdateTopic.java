package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateTopicEvent;
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