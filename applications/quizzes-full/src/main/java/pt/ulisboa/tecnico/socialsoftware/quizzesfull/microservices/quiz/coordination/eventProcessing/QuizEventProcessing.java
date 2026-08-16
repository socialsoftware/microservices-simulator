package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.functionalities.QuizFunctionalities;

@Service
public class QuizEventProcessing {

    @Autowired
    private QuizFunctionalities quizFunctionalities;

    public void processUpdateQuestionEvent(Integer aggregateId, UpdateQuestionEvent event) {
        quizFunctionalities.updateQuestionInQuizByEvent(aggregateId, event.getPublisherAggregateId(), event.getTitle(), event.getContent());
    }

    public void processDeleteQuestionEvent(Integer aggregateId, DeleteQuestionEvent event) {
        quizFunctionalities.removeQuestionFromQuizByEvent(aggregateId, event.getPublisherAggregateId());
    }

    public void processDeleteCourseExecutionEvent(Integer aggregateId, DeleteCourseExecutionEvent event) {
        quizFunctionalities.invalidateQuizByEvent(aggregateId);
    }
}
