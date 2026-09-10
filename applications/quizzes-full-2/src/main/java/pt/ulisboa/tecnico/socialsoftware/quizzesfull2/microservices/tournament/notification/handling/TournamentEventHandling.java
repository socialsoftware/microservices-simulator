package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.handling.handlers.TournamentEventHandler;

@Component
public class TournamentEventHandling implements EventHandling {

    @Autowired
    private EventApplicationService eventApplicationService;

    @Autowired
    private TournamentEventHandler tournamentEventHandler;

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateStudentNameEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateStudentNameEvent.class, tournamentEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleAnonymizeStudentEvents() {
        eventApplicationService.handleSubscribedEvent(AnonymizeStudentEvent.class, tournamentEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteUserEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteUserEvent.class, tournamentEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateTopicEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateTopicEvent.class, tournamentEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteTopicEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteTopicEvent.class, tournamentEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteCourseExecutionEvent.class, tournamentEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDisenrollStudentFromCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DisenrollStudentFromCourseExecutionEvent.class,
                tournamentEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleInvalidateQuizEvents() {
        eventApplicationService.handleSubscribedEvent(InvalidateQuizEvent.class, tournamentEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleQuizAnswerQuestionAnswerEvents() {
        eventApplicationService.handleSubscribedEvent(QuizAnswerQuestionAnswerEvent.class,
                tournamentEventHandler);
    }
}
