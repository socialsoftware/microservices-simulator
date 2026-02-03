package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.TournamentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.ExecutionDeletedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.ExecutionUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.ExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.TopicUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.TopicDeletedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TopicDeletedEvent;

@Component
public class TournamentEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private TournamentEventProcessing tournamentEventProcessing;
    @Autowired
    private TournamentRepository tournamentRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleExecutionDeletedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ExecutionDeletedEvent.class,
                new ExecutionDeletedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleExecutionUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ExecutionUpdatedEvent.class,
                new ExecutionUpdatedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleTopicUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(TopicUpdatedEvent.class,
                new TopicUpdatedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleTopicDeletedEventEvents() {
        eventApplicationService.handleSubscribedEvent(TopicDeletedEvent.class,
                new TopicDeletedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

}