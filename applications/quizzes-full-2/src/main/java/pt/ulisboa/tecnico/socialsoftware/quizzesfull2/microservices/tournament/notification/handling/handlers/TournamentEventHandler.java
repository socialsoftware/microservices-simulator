package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.handling.handlers;

import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing;

@Component
public class TournamentEventHandler extends EventHandler {

    private final TournamentEventProcessing tournamentEventProcessing;

    public TournamentEventHandler(TournamentRepository tournamentRepository,
                                  TournamentEventProcessing tournamentEventProcessing) {
        super(tournamentRepository);
        this.tournamentEventProcessing = tournamentEventProcessing;
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof UpdateStudentNameEvent e) {
            tournamentEventProcessing.processUpdateStudentNameEvent(subscriberAggregateId, e);
        } else if (event instanceof AnonymizeStudentEvent e) {
            tournamentEventProcessing.processAnonymizeStudentEvent(subscriberAggregateId, e);
        } else if (event instanceof DeleteUserEvent e) {
            tournamentEventProcessing.processDeleteUserEvent(subscriberAggregateId, e);
        } else if (event instanceof UpdateTopicEvent e) {
            tournamentEventProcessing.processUpdateTopicEvent(subscriberAggregateId, e);
        } else if (event instanceof DeleteTopicEvent e) {
            tournamentEventProcessing.processDeleteTopicEvent(subscriberAggregateId, e);
        } else if (event instanceof DeleteCourseExecutionEvent e) {
            tournamentEventProcessing.processDeleteCourseExecutionEvent(subscriberAggregateId, e);
        } else if (event instanceof DisenrollStudentFromCourseExecutionEvent e) {
            tournamentEventProcessing.processDisenrollStudentFromCourseExecutionEvent(subscriberAggregateId, e);
        } else if (event instanceof InvalidateQuizEvent e) {
            tournamentEventProcessing.processInvalidateQuizEvent(subscriberAggregateId, e);
        } else if (event instanceof QuizAnswerQuestionAnswerEvent e) {
            tournamentEventProcessing.processQuizAnswerQuestionAnswerEvent(subscriberAggregateId, e);
        }
    }
}
