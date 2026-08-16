package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.functionalities.QuizAnswerFunctionalities;

@Service
public class QuizAnswerEventProcessing {

    @Autowired
    private QuizAnswerFunctionalities quizAnswerFunctionalities;

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent event) {
        quizAnswerFunctionalities.removeQuizAnswerByEvent(aggregateId);
    }

    public void processUpdateStudentNameEvent(Integer aggregateId, UpdateStudentNameEvent event) {
        quizAnswerFunctionalities.updateStudentNameByEvent(aggregateId, event.getUpdatedName());
    }

    public void processAnonymizeStudentEvent(Integer aggregateId, AnonymizeStudentEvent event) {
        quizAnswerFunctionalities.anonymizeStudentByEvent(aggregateId, event.getName(), event.getUsername());
    }

    public void processDisenrollStudentFromCourseExecutionEvent(Integer aggregateId, DisenrollStudentFromCourseExecutionEvent event) {
        quizAnswerFunctionalities.removeQuizAnswerIfDisenrolledByEvent(aggregateId, event.getUserId());
    }

    public void processUpdateQuestionEvent(Integer aggregateId, UpdateQuestionEvent event) {
        quizAnswerFunctionalities.updateQuestionVersionByEvent(aggregateId, event.getPublisherAggregateId(), event.getPublisherAggregateVersion());
    }

    public void processDeleteCourseExecutionEvent(Integer aggregateId, DeleteCourseExecutionEvent event) {
        quizAnswerFunctionalities.removeQuizAnswerByEvent(aggregateId);
    }

    public void processInvalidateQuizEvent(Integer aggregateId, InvalidateQuizEvent event) {
        quizAnswerFunctionalities.removeQuizAnswerByEvent(aggregateId);
    }
}
