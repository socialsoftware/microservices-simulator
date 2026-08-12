package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.notification.handling.handlers;

import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.eventProcessing.QuizAnswerEventProcessing;

@Component
public class QuizAnswerEventHandler extends EventHandler {

    private final QuizAnswerEventProcessing quizAnswerEventProcessing;

    public QuizAnswerEventHandler(QuizAnswerRepository quizAnswerRepository,
                                  QuizAnswerEventProcessing quizAnswerEventProcessing) {
        super(quizAnswerRepository);
        this.quizAnswerEventProcessing = quizAnswerEventProcessing;
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof UpdateStudentNameEvent e) {
            quizAnswerEventProcessing.processUpdateStudentNameEvent(subscriberAggregateId, e);
        } else if (event instanceof AnonymizeStudentEvent e) {
            quizAnswerEventProcessing.processAnonymizeStudentEvent(subscriberAggregateId, e);
        } else if (event instanceof DeleteUserEvent e) {
            quizAnswerEventProcessing.processDeleteUserEvent(subscriberAggregateId, e);
        } else if (event instanceof UpdateQuestionEvent e) {
            quizAnswerEventProcessing.processUpdateQuestionEvent(subscriberAggregateId, e);
        } else if (event instanceof DeleteCourseExecutionEvent e) {
            quizAnswerEventProcessing.processDeleteCourseExecutionEvent(subscriberAggregateId, e);
        } else if (event instanceof DisenrollStudentFromCourseExecutionEvent e) {
            quizAnswerEventProcessing.processDisenrollStudentFromCourseExecutionEvent(subscriberAggregateId, e);
        } else if (event instanceof InvalidateQuizEvent e) {
            quizAnswerEventProcessing.processInvalidateQuizEvent(subscriberAggregateId, e);
        }
    }
}
