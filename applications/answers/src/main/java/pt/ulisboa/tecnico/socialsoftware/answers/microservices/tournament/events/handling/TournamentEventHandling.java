package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.ExecutionUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.ExecutionUserUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.TopicUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.QuizUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuizUpdatedEvent;

@Component
public class TournamentEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private TournamentEventProcessing tournamentEventProcessing;
    @Autowired
    private TournamentRepository tournamentRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleExecutionUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ExecutionUpdatedEvent.class,
                new ExecutionUpdatedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleExecutionUserUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ExecutionUserUpdatedEvent.class,
                new ExecutionUserUpdatedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleTopicUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(TopicUpdatedEvent.class,
                new TopicUpdatedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleQuizUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(QuizUpdatedEvent.class,
                new QuizUpdatedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

}