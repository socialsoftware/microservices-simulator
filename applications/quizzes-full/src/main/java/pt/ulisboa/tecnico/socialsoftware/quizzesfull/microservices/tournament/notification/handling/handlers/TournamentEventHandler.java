package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.handling.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing;

@Component
public class TournamentEventHandler extends EventHandler {

    @Autowired
    private TournamentEventProcessing tournamentEventProcessing;

    @Autowired
    public TournamentEventHandler(TournamentRepository repository) {
        super(repository);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof DeleteUserEvent) {
            tournamentEventProcessing.processDeleteUserEvent(subscriberAggregateId, (DeleteUserEvent) event);
        } else if (event instanceof UpdateStudentNameEvent) {
            tournamentEventProcessing.processUpdateStudentNameEvent(subscriberAggregateId, (UpdateStudentNameEvent) event);
        } else if (event instanceof AnonymizeStudentEvent) {
            tournamentEventProcessing.processAnonymizeStudentEvent(subscriberAggregateId, (AnonymizeStudentEvent) event);
        } else if (event instanceof UpdateTopicEvent) {
            tournamentEventProcessing.processUpdateTopicEvent(subscriberAggregateId, (UpdateTopicEvent) event);
        } else if (event instanceof DeleteTopicEvent) {
            tournamentEventProcessing.processDeleteTopicEvent(subscriberAggregateId, (DeleteTopicEvent) event);
        } else if (event instanceof DeleteCourseExecutionEvent) {
            tournamentEventProcessing.processDeleteCourseExecutionEvent(subscriberAggregateId, (DeleteCourseExecutionEvent) event);
        } else if (event instanceof InvalidateQuizEvent) {
            tournamentEventProcessing.processInvalidateQuizEvent(subscriberAggregateId, (InvalidateQuizEvent) event);
        } else if (event instanceof QuizAnswerQuestionAnswerEvent) {
            tournamentEventProcessing.processQuizAnswerQuestionAnswerEvent(subscriberAggregateId, (QuizAnswerQuestionAnswerEvent) event);
        }
    }
}
