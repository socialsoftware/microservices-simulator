package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.eventProcessing;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;

@Service
public class TournamentEventProcessing {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TournamentEventProcessing.class);

    @Autowired
    private TournamentFunctionalities tournamentFunctionalities;

    public void processAnonymizeStudentEvent(Integer aggregateId, AnonymizeStudentEvent anonymizeEvent) {
        logger.info("Processing AnonymizeStudentEvent: aggregateId={}, event={}", aggregateId, anonymizeEvent);
        tournamentFunctionalities.anonymizeStudent(aggregateId, anonymizeEvent);
    }

    public void processRemoveCourseExecutionEvent(Integer aggregateId, DeleteCourseExecutionEvent deleteCourseExecutionEvent) {
        logger.info("Processing RemoveCourseExecutionEvent: aggregateId={}, event={}", aggregateId, deleteCourseExecutionEvent);
        tournamentFunctionalities.removeCourseExecution(aggregateId, deleteCourseExecutionEvent);
    }

    public void processUpdateTopicEvent(Integer aggregateId, UpdateTopicEvent updateTopicEvent) {
        logger.info("Processing UpdateTopicEvent: aggregateId={}, event={}", aggregateId, updateTopicEvent);
        tournamentFunctionalities.updateTopic(aggregateId, updateTopicEvent);
    }

    public void processDeleteTopicEvent(Integer aggregateId, DeleteTopicEvent deleteTopicEvent) {
        logger.info("Processing DeleteTopicEvent: aggregateId={}, event={}", aggregateId, deleteTopicEvent);
        tournamentFunctionalities.deleteTopic(aggregateId, deleteTopicEvent);
    }

    public void processAnswerQuestionEvent(Integer aggregateId, QuizAnswerQuestionAnswerEvent quizAnswerQuestionAnswerEvent) {
        logger.info("Processing QuizAnswerQuestionAnswerEvent: aggregateId={}, event={}", aggregateId, quizAnswerQuestionAnswerEvent);
        tournamentFunctionalities.updateParticipantAnswer(aggregateId, quizAnswerQuestionAnswerEvent);
    }

    public void processDisenrollStudentFromCourseExecutionEvent(Integer aggregateId, DisenrollStudentFromCourseExecutionEvent disenrollStudentFromCourseExecutionEvent) {
        logger.info("Processing DisenrollStudentFromCourseExecutionEvent: aggregateId={}, event={}", aggregateId, disenrollStudentFromCourseExecutionEvent);
        tournamentFunctionalities.disenrollStudent(aggregateId, disenrollStudentFromCourseExecutionEvent);
    }

    public void processInvalidateQuizEvent(Integer aggregateId, InvalidateQuizEvent invalidateQuizEvent) {
        logger.info("Processing InvalidateQuizEvent: aggregateId={}, event={}", aggregateId, invalidateQuizEvent);
        tournamentFunctionalities.invalidateQuiz(aggregateId, invalidateQuizEvent);
    }

    public void processUpdateStudentNameEvent(Integer subscriberAggregateId, UpdateStudentNameEvent updateStudentNameEvent) {
        logger.info("Processing UpdateStudentNameEvent: subscriberAggregateId={}, event={}", subscriberAggregateId, updateStudentNameEvent);
        tournamentFunctionalities.updateStudentName(subscriberAggregateId, updateStudentNameEvent);
    }
}
