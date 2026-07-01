package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.functionalities.QuizAnswerFunctionalities;

@Service
public class QuizAnswerEventProcessing {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(QuizAnswerEventProcessing.class);

    @Autowired
    private QuizAnswerFunctionalities quizAnswerFunctionalities;

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent deleteUserEvent) {
        logger.info("Processing DeleteUserEvent: aggregateId={}, event={}", aggregateId, deleteUserEvent);
        quizAnswerFunctionalities.removeUserFromQuizAnswer(aggregateId, deleteUserEvent.getPublisherAggregateId(), deleteUserEvent.getPublisherAggregateVersion());
    }

    public void processDisenrollStudentEvent(Integer aggregateId, DisenrollStudentFromCourseExecutionEvent disenrollStudentFromCourseExecutionEvent) {
        logger.info("Processing DisenrollStudentEvent: aggregateId={}, event={}", aggregateId, disenrollStudentFromCourseExecutionEvent);
        quizAnswerFunctionalities.removeUserFromQuizAnswer(aggregateId, disenrollStudentFromCourseExecutionEvent.getPublisherAggregateId(), disenrollStudentFromCourseExecutionEvent.getPublisherAggregateVersion());
    }

    public void processUpdateStudentNameEvent(Integer subscriberAggregateId, UpdateStudentNameEvent updateStudentNameEvent) {
        logger.info("Processing UpdateStudentNameEvent: subscriberAggregateId={}, event={}", subscriberAggregateId, updateStudentNameEvent);
        quizAnswerFunctionalities.updateUserNameInQuizAnswer(subscriberAggregateId, updateStudentNameEvent.getPublisherAggregateId(), updateStudentNameEvent.getPublisherAggregateVersion(), updateStudentNameEvent.getStudentAggregateId(), updateStudentNameEvent.getUpdatedName());
    }

    public void processDeleteCourseExecutionEvent(Integer aggregateId, DeleteCourseExecutionEvent deleteCourseExecutionEvent) {
        logger.info("Processing DeleteCourseExecutionEvent: aggregateId={}, event={}", aggregateId, deleteCourseExecutionEvent);
        quizAnswerFunctionalities.removeQuizAnswer(aggregateId);
    }

    public void processInvalidateQuizEvent(Integer aggregateId, InvalidateQuizEvent invalidateQuizEvent) {
        logger.info("Processing InvalidateQuizEvent: aggregateId={}, event={}", aggregateId, invalidateQuizEvent);
        quizAnswerFunctionalities.removeQuizAnswer(aggregateId);
    }

    public void processAnonymizeStudentEvent(Integer aggregateId, AnonymizeStudentEvent anonymizeStudentEvent) {
        logger.info("Processing AnonymizeStudentEvent: aggregateId={}, event={}", aggregateId, anonymizeStudentEvent);
        quizAnswerFunctionalities.updateUserNameInQuizAnswer(aggregateId, anonymizeStudentEvent.getPublisherAggregateId(),
                anonymizeStudentEvent.getPublisherAggregateVersion(), anonymizeStudentEvent.getStudentAggregateId(),
                anonymizeStudentEvent.getName());
    }
}
