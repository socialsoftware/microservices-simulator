package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.eventProcessing;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.publish.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.publish.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities;

@Service
public class QuizEventProcessing {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(QuizEventProcessing.class);

    @Autowired
    private QuizFunctionalities quizFunctionalities;

    public void processDeleteCourseExecutionEvent(Integer aggregateId, DeleteCourseExecutionEvent deleteCourseExecutionEvent) {
        logger.info("Processing DeleteCourseExecutionEvent: aggregateId={}, event={}", aggregateId, deleteCourseExecutionEvent);
        quizFunctionalities.removeCourseExecutionFromQuiz(aggregateId, deleteCourseExecutionEvent);
    }

    public void processUpdateQuestionEvent(Integer aggregateId, UpdateQuestionEvent updateQuestionEvent) {
        logger.info("Processing UpdateQuestionEvent: aggregateId={}, event={}", aggregateId, updateQuestionEvent);
        quizFunctionalities.updateQuestionInQuiz(aggregateId, updateQuestionEvent);
    }

    public void processDeleteQuizQuestionEvent(Integer aggregateId, DeleteQuestionEvent deleteQuestionEvent) {
        logger.info("Processing DeleteQuizQuestionEvent: aggregateId={}, event={}", aggregateId, deleteQuestionEvent);
        quizFunctionalities.removeQuizQuestion(aggregateId, deleteQuestionEvent);
    }
}
