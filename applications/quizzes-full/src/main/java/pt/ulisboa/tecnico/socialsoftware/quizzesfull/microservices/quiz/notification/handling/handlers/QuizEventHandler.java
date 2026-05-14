package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.notification.handling.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.eventProcessing.QuizEventProcessing;

@Component
public class QuizEventHandler extends EventHandler {

    @Autowired
    private QuizEventProcessing quizEventProcessing;

    @Autowired
    public QuizEventHandler(QuizRepository repository) {
        super(repository);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof UpdateQuestionEvent) {
            quizEventProcessing.processUpdateQuestionEvent(subscriberAggregateId, (UpdateQuestionEvent) event);
        } else if (event instanceof DeleteQuestionEvent) {
            quizEventProcessing.processDeleteQuestionEvent(subscriberAggregateId, (DeleteQuestionEvent) event);
        } else if (event instanceof DeleteCourseExecutionEvent) {
            quizEventProcessing.processDeleteCourseExecutionEvent(subscriberAggregateId, (DeleteCourseExecutionEvent) event);
        }
    }
}
