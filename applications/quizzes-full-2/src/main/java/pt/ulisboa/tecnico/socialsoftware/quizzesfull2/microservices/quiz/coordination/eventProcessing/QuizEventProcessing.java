package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.functionalities.QuizFunctionalities;

@Service
public class QuizEventProcessing {

    @Autowired
    private QuizFunctionalities quizFunctionalities;

    public void processUpdateQuestionEvent(Integer aggregateId, UpdateQuestionEvent event) {
        quizFunctionalities.setQuestionDetailsByEvent(aggregateId, event.getQuestionAggregateId(),
                event.getTitle(), event.getContent(), event.getPublisherAggregateVersion());
    }

    public void processDeleteQuestionEvent(Integer aggregateId, DeleteQuestionEvent event) {
        quizFunctionalities.invalidateForDeletedQuestionByEvent(aggregateId, event.getQuestionAggregateId());
    }

    public void processDeleteCourseExecutionEvent(Integer aggregateId, DeleteCourseExecutionEvent event) {
        quizFunctionalities.removeForDeletedExecutionByEvent(aggregateId, event.getExecutionAggregateId());
    }
}
