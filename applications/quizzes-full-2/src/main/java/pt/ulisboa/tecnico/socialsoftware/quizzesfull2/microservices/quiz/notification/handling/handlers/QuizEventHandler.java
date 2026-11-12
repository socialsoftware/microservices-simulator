package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.notification.handling.handlers;

import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.eventProcessing.QuizEventProcessing;

@Component
public class QuizEventHandler extends EventHandler {

    private final QuizEventProcessing quizEventProcessing;

    public QuizEventHandler(QuizRepository quizRepository, QuizEventProcessing quizEventProcessing) {
        super(quizRepository);
        this.quizEventProcessing = quizEventProcessing;
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof UpdateQuestionEvent e) {
            quizEventProcessing.processUpdateQuestionEvent(subscriberAggregateId, e);
        } else if (event instanceof DeleteQuestionEvent e) {
            quizEventProcessing.processDeleteQuestionEvent(subscriberAggregateId, e);
        } else if (event instanceof DeleteCourseExecutionEvent e) {
            quizEventProcessing.processDeleteCourseExecutionEvent(subscriberAggregateId, e);
        }
    }
}
