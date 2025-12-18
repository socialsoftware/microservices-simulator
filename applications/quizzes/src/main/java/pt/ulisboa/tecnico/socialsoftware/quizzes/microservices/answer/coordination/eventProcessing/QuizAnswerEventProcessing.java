package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DisenrollStudentFromCourseExecutionEvent;
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

    public void processDeleteQuestionEvent(Integer aggregateId, DeleteQuestionEvent deleteQuestionEvent) {
        logger.info("Processing DeleteQuestionEvent: aggregateId={}, event={}", aggregateId, deleteQuestionEvent);
        quizAnswerFunctionalities.removeQuestionFromQuizAnswer(aggregateId, deleteQuestionEvent.getPublisherAggregateId(), deleteQuestionEvent.getPublisherAggregateVersion());
    }

    public void processDisenrollStudentEvent(Integer aggregateId, DisenrollStudentFromCourseExecutionEvent disenrollStudentFromCourseExecutionEvent) {
        logger.info("Processing DisenrollStudentEvent: aggregateId={}, event={}", aggregateId, disenrollStudentFromCourseExecutionEvent);
        quizAnswerFunctionalities.removeUserFromQuizAnswer(aggregateId, disenrollStudentFromCourseExecutionEvent.getPublisherAggregateId(), disenrollStudentFromCourseExecutionEvent.getPublisherAggregateVersion());
    }

    public void processUpdateStudentNameEvent(Integer subscriberAggregateId, UpdateStudentNameEvent updateStudentNameEvent) {
        logger.info("Processing UpdateStudentNameEvent: subscriberAggregateId={}, event={}", subscriberAggregateId, updateStudentNameEvent);
        quizAnswerFunctionalities.updateUserNameInQuizAnswer(subscriberAggregateId, updateStudentNameEvent.getPublisherAggregateId(), updateStudentNameEvent.getPublisherAggregateVersion(), updateStudentNameEvent.getStudentAggregateId(), updateStudentNameEvent.getUpdatedName());
    }
}
