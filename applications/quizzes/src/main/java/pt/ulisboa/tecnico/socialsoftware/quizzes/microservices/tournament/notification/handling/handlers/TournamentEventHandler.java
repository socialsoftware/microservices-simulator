package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing;

import java.util.HashSet;
import java.util.Set;

public abstract class TournamentEventHandler extends EventHandler {
    private final TournamentRepository tournamentRepository;
    protected TournamentEventProcessing tournamentEventProcessing;

    public TournamentEventHandler(TournamentRepository tournamentRepository, TournamentEventProcessing tournamentEventProcessing) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentEventProcessing = tournamentEventProcessing;
    }

    @Override
    public Set<Integer> getAggregateIds() {
        return tournamentRepository.findAllAggregateIds();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions(Integer subscriberAggregateId, Class<? extends Event> eventClass) {
        return tournamentRepository.findLastAggregateVersion(subscriberAggregateId)
                .map(aggregate -> aggregate.getEventSubscriptionsByEventType(eventClass.getSimpleName()))
                .orElse(new HashSet<>());
    }

}
