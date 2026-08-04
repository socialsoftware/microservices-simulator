package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.handling.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.eventProcessing.QuizAnswerEventProcessing;

@Component
public class QuizAnswerEventHandler extends EventHandler {

    @Autowired
    private QuizAnswerEventProcessing quizAnswerEventProcessing;

    @Autowired
    public QuizAnswerEventHandler(QuizAnswerRepository repository) {
        super(repository);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof DeleteUserEvent) {
            quizAnswerEventProcessing.processDeleteUserEvent(subscriberAggregateId, (DeleteUserEvent) event);
        } else if (event instanceof UpdateStudentNameEvent) {
            quizAnswerEventProcessing.processUpdateStudentNameEvent(subscriberAggregateId, (UpdateStudentNameEvent) event);
        } else if (event instanceof AnonymizeStudentEvent) {
            quizAnswerEventProcessing.processAnonymizeStudentEvent(subscriberAggregateId, (AnonymizeStudentEvent) event);
        } else if (event instanceof DisenrollStudentFromCourseExecutionEvent) {
            quizAnswerEventProcessing.processDisenrollStudentFromCourseExecutionEvent(subscriberAggregateId, (DisenrollStudentFromCourseExecutionEvent) event);
        } else if (event instanceof UpdateQuestionEvent) {
            quizAnswerEventProcessing.processUpdateQuestionEvent(subscriberAggregateId, (UpdateQuestionEvent) event);
        } else if (event instanceof DeleteCourseExecutionEvent) {
            quizAnswerEventProcessing.processDeleteCourseExecutionEvent(subscriberAggregateId, (DeleteCourseExecutionEvent) event);
        } else if (event instanceof InvalidateQuizEvent) {
            quizAnswerEventProcessing.processInvalidateQuizEvent(subscriberAggregateId, (InvalidateQuizEvent) event);
        }
    }
}
