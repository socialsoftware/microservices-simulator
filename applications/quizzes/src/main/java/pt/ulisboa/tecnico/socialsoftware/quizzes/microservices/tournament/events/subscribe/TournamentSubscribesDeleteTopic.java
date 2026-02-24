package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentTopic;

public class TournamentSubscribesDeleteTopic extends EventSubscription {
    public TournamentSubscribesDeleteTopic(TournamentTopic tournamentTopic) {
        super(tournamentTopic.getTopicAggregateId(),
                tournamentTopic.getTopicVersion(),
                DeleteTopicEvent.class.getSimpleName());
    }

    public TournamentSubscribesDeleteTopic() {}

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
