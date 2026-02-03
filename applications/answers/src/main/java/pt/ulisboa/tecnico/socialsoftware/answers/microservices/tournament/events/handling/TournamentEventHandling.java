package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.TournamentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.ExecutionUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.ExecutionDeletedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.ExecutionUserUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.ExecutionUserDeletedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.TopicUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.TopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.TopicDeletedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.TopicDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.QuizUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers.QuizDeletedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizDeletedEvent;

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
    public void handleExecutionDeletedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ExecutionDeletedEvent.class,
                new ExecutionDeletedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleExecutionUserUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ExecutionUserUpdatedEvent.class,
                new ExecutionUserUpdatedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleExecutionUserDeletedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ExecutionUserDeletedEvent.class,
                new ExecutionUserDeletedEventHandler(tournamentRepository, tournamentEventProcessing));
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

    @Scheduled(fixedDelay = 1000)
    public void handleQuizUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(QuizUpdatedEvent.class,
                new QuizUpdatedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleQuizDeletedEventEvents() {
        eventApplicationService.handleSubscribedEvent(QuizDeletedEvent.class,
                new QuizDeletedEventHandler(tournamentRepository, tournamentEventProcessing));
    }

}