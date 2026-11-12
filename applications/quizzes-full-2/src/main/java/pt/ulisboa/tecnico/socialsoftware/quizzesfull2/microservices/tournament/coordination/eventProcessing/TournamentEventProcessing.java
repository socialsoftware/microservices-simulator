package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.functionalities.TournamentFunctionalities;

@Service
public class TournamentEventProcessing {

    @Autowired
    private TournamentFunctionalities tournamentFunctionalities;

    public void processUpdateStudentNameEvent(Integer aggregateId, UpdateStudentNameEvent event) {
        tournamentFunctionalities.setUserNameByEvent(aggregateId, event.getStudentAggregateId(),
                event.getUpdatedName(), event.getPublisherAggregateVersion());
    }

    public void processAnonymizeStudentEvent(Integer aggregateId, AnonymizeStudentEvent event) {
        tournamentFunctionalities.anonymizeUserByEvent(aggregateId, event.getStudentAggregateId(),
                event.getName(), event.getUsername(), event.getPublisherAggregateVersion());
    }

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent event) {
        tournamentFunctionalities.removeForDeletedUserByEvent(aggregateId, event.getUserAggregateId());
    }

    public void processUpdateTopicEvent(Integer aggregateId, UpdateTopicEvent event) {
        tournamentFunctionalities.setTopicNameByEvent(aggregateId, event.getTopicAggregateId(),
                event.getTopicName(), event.getPublisherAggregateVersion());
    }

    public void processDeleteTopicEvent(Integer aggregateId, DeleteTopicEvent event) {
        tournamentFunctionalities.removeDeletedTopicByEvent(aggregateId, event.getTopicAggregateId());
    }

    public void processDeleteCourseExecutionEvent(Integer aggregateId, DeleteCourseExecutionEvent event) {
        tournamentFunctionalities.removeForDeletedExecutionByEvent(aggregateId,
                event.getExecutionAggregateId());
    }

    public void processDisenrollStudentFromCourseExecutionEvent(
            Integer aggregateId, DisenrollStudentFromCourseExecutionEvent event) {
        tournamentFunctionalities.removeDisenrolledParticipantByEvent(aggregateId,
                event.getExecutionAggregateId(), event.getStudentAggregateId());
    }

    public void processInvalidateQuizEvent(Integer aggregateId, InvalidateQuizEvent event) {
        tournamentFunctionalities.removeForInvalidatedQuizByEvent(aggregateId, event.getQuizAggregateId());
    }

    public void processQuizAnswerQuestionAnswerEvent(Integer aggregateId, QuizAnswerQuestionAnswerEvent event) {
        tournamentFunctionalities.recordQuestionAnswerByEvent(aggregateId, event.getQuizAggregateId(),
                event.getStudentAggregateId(), event.getQuizAnswerAggregateId(), event.getCorrect(),
                event.getAnswerTime(), event.getPublisherAggregateVersion());
    }
}
