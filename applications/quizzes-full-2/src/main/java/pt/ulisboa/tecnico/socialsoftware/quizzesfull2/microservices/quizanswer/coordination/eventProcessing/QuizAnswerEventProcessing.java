package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.functionalities.QuizAnswerFunctionalities;

@Service
public class QuizAnswerEventProcessing {

    @Autowired
    private QuizAnswerFunctionalities quizAnswerFunctionalities;

    public void processUpdateStudentNameEvent(Integer aggregateId, UpdateStudentNameEvent event) {
        quizAnswerFunctionalities.setStudentNameByEvent(aggregateId, event.getStudentAggregateId(),
                event.getUpdatedName(), event.getPublisherAggregateVersion());
    }

    public void processAnonymizeStudentEvent(Integer aggregateId, AnonymizeStudentEvent event) {
        quizAnswerFunctionalities.anonymizeStudentByEvent(aggregateId, event.getStudentAggregateId(),
                event.getName(), event.getPublisherAggregateVersion());
    }

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent event) {
        quizAnswerFunctionalities.removeForDeletedStudentByEvent(aggregateId, event.getUserAggregateId());
    }

    public void processUpdateQuestionEvent(Integer aggregateId, UpdateQuestionEvent event) {
        quizAnswerFunctionalities.setQuestionVersionByEvent(aggregateId, event.getQuestionAggregateId(),
                event.getPublisherAggregateVersion());
    }

    public void processDeleteCourseExecutionEvent(Integer aggregateId, DeleteCourseExecutionEvent event) {
        quizAnswerFunctionalities.removeForDeletedExecutionByEvent(aggregateId, event.getExecutionAggregateId());
    }

    public void processDisenrollStudentFromCourseExecutionEvent(
            Integer aggregateId, DisenrollStudentFromCourseExecutionEvent event) {
        quizAnswerFunctionalities.removeForDisenrolledStudentByEvent(aggregateId,
                event.getExecutionAggregateId(), event.getStudentAggregateId());
    }

    public void processInvalidateQuizEvent(Integer aggregateId, InvalidateQuizEvent event) {
        quizAnswerFunctionalities.removeForInvalidatedQuizByEvent(aggregateId, event.getQuizAggregateId());
    }
}
