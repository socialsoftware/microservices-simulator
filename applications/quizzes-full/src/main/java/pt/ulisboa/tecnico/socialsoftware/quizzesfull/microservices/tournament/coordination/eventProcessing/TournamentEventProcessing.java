package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.functionalities.TournamentFunctionalities;

@Service
public class TournamentEventProcessing {

    @Autowired
    private TournamentFunctionalities tournamentFunctionalities;

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent event) {
        tournamentFunctionalities.removeUserFromTournamentByEvent(aggregateId, event.getPublisherAggregateId());
    }

    public void processUpdateStudentNameEvent(Integer aggregateId, UpdateStudentNameEvent event) {
        tournamentFunctionalities.updateStudentNameByEvent(aggregateId, event.getStudentAggregateId(), event.getUpdatedName());
    }

    public void processAnonymizeStudentEvent(Integer aggregateId, AnonymizeStudentEvent event) {
        tournamentFunctionalities.anonymizeStudentByEvent(aggregateId, event.getStudentAggregateId(), event.getName(), event.getUsername());
    }

    public void processUpdateTopicEvent(Integer aggregateId, UpdateTopicEvent event) {
        tournamentFunctionalities.updateTopicNameByEvent(aggregateId, event.getPublisherAggregateId(), event.getTopicName());
    }

    public void processDeleteTopicEvent(Integer aggregateId, DeleteTopicEvent event) {
        tournamentFunctionalities.removeTopicByEvent(aggregateId, event.getPublisherAggregateId());
    }

    public void processDeleteCourseExecutionEvent(Integer aggregateId, DeleteCourseExecutionEvent event) {
        tournamentFunctionalities.removeTournamentByExecutionByEvent(aggregateId);
    }

    public void processInvalidateQuizEvent(Integer aggregateId, InvalidateQuizEvent event) {
        tournamentFunctionalities.removeTournamentByQuizByEvent(aggregateId);
    }

    public void processQuizAnswerQuestionAnswerEvent(Integer aggregateId, QuizAnswerQuestionAnswerEvent event) {
        tournamentFunctionalities.updateParticipantAnsweredByEvent(aggregateId, event.getUserAggregateId());
    }
}
