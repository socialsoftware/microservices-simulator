package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.eventProcessing;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.publish.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.publish.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AnonymizeUserTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateUserNameFunctionalitySagas;

import java.util.Arrays;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;

@Service
public class TournamentEventProcessing {
    @Autowired
    private TournamentService tournamentService;

    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private Environment env;

    private TransactionalModel workflowType;
    @Autowired
    private CommandGateway commandGateway;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else if (Arrays.asList(activeProfiles).contains(TCC.getValue())) {
            workflowType = TCC;
        } else {
            throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processAnonymizeStudentEvent(Integer aggregateId, AnonymizeStudentEvent anonymizeEvent) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

                AnonymizeUserTournamentFunctionalitySagas anonymizeUserTournamentFunctionalitySagas =
                        new AnonymizeUserTournamentFunctionalitySagas(unitOfWorkService, aggregateId, anonymizeEvent.getPublisherAggregateId(), anonymizeEvent.getStudentAggregateId(), anonymizeEvent.getName(), anonymizeEvent.getUsername(), anonymizeEvent.getPublisherAggregateVersion(), unitOfWork, commandGateway);
                anonymizeUserTournamentFunctionalitySagas.executeWorkflow(unitOfWork);
                break;
            case TCC:
                unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
                tournamentService.anonymizeUser(aggregateId, anonymizeEvent.getPublisherAggregateId(), anonymizeEvent.getStudentAggregateId(), anonymizeEvent.getName(), anonymizeEvent.getUsername(), anonymizeEvent.getPublisherAggregateVersion(), unitOfWork);
                unitOfWorkService.commit(unitOfWork);
                break;
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }
    public void processRemoveCourseExecutionEvent(Integer aggregateId, DeleteCourseExecutionEvent deleteCourseExecutionEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.removeCourseExecution(aggregateId, deleteCourseExecutionEvent.getPublisherAggregateId(), deleteCourseExecutionEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
    public void processUpdateTopicEvent(Integer aggregateId, UpdateTopicEvent updateTopicEvent){
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.updateTopic(aggregateId, updateTopicEvent.getPublisherAggregateId(), updateTopicEvent.getTopicName(), updateTopicEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
    public void processDeleteTopicEvent(Integer aggregateId, DeleteTopicEvent deleteTopicEvent){
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.removeTopic(aggregateId, deleteTopicEvent.getPublisherAggregateId(), deleteTopicEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
    public void processAnswerQuestionEvent(Integer aggregateId, QuizAnswerQuestionAnswerEvent quizAnswerQuestionAnswerEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.updateParticipantAnswer(aggregateId, quizAnswerQuestionAnswerEvent.getStudentAggregateId(), quizAnswerQuestionAnswerEvent.getPublisherAggregateId(), quizAnswerQuestionAnswerEvent.getQuestionAggregateId(), quizAnswerQuestionAnswerEvent.isCorrect(), quizAnswerQuestionAnswerEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
    public void processDisenrollStudentFromCourseExecutionEvent(Integer aggregateId, DisenrollStudentFromCourseExecutionEvent disenrollStudentFromCourseExecutionEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.removeUser(aggregateId, disenrollStudentFromCourseExecutionEvent.getPublisherAggregateId(), disenrollStudentFromCourseExecutionEvent.getStudentAggregateId() , disenrollStudentFromCourseExecutionEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
    public void processInvalidateQuizEvent(Integer aggregateId, InvalidateQuizEvent invalidateQuizEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.invalidateQuiz(aggregateId, invalidateQuizEvent.getPublisherAggregateId(), invalidateQuizEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
    public void processUpdateStudentNameEvent(Integer subscriberAggregateId, UpdateStudentNameEvent updateStudentNameEvent) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();


        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

                UpdateUserNameFunctionalitySagas updateUserNameFunctionalitySagas =
                        new UpdateUserNameFunctionalitySagas(sagaUnitOfWorkService,updateStudentNameEvent.getPublisherAggregateVersion(), subscriberAggregateId ,updateStudentNameEvent.getPublisherAggregateId() ,updateStudentNameEvent.getStudentAggregateId() ,sagaUnitOfWork, updateStudentNameEvent.getUpdatedName(), commandGateway);
                                                      
                updateUserNameFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
                tournamentService.updateUserName(subscriberAggregateId, updateStudentNameEvent.getPublisherAggregateId(), updateStudentNameEvent.getPublisherAggregateVersion(), updateStudentNameEvent.getStudentAggregateId(), updateStudentNameEvent.getUpdatedName(), unitOfWork);
                unitOfWorkService.commit(unitOfWork);
                break;
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }

        
    }
}
