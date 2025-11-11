package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.eventProcessing;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.publish.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.publish.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AnonymizeUserTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateUserNameFunctionalitySagas;

import java.util.Arrays;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;

@Service
public class TournamentEventProcessing {
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired
    private Environment env;

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TournamentEventProcessing.class);

    private TransactionalModel workflowType;
    @Autowired
    private CommandGateway commandGateway;

    public TournamentEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

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

    public void processAnonymizeStudentEvent(Integer aggregateId, AnonymizeStudentEvent anonymizeEvent) {
        logger.info("Processing AnonymizeStudentEvent: aggregateId={}, event={}", aggregateId, anonymizeEvent);
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        UnitOfWork unitOfWork;
        switch (workflowType) {
            case SAGAS:
                unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
                AnonymizeUserTournamentFunctionalitySagas anonymizeUserTournamentFunctionalitySagas = new AnonymizeUserTournamentFunctionalitySagas(
                        unitOfWorkService, aggregateId, anonymizeEvent.getPublisherAggregateId(),
                        anonymizeEvent.getStudentAggregateId(), anonymizeEvent.getName(), anonymizeEvent.getUsername(),
                        anonymizeEvent.getPublisherAggregateVersion(), unitOfWork, commandGateway);
                anonymizeUserTournamentFunctionalitySagas.executeWorkflow(unitOfWork);
                break;
            case TCC:
                unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
                var command = new AnonymizeUserCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                        aggregateId, anonymizeEvent.getPublisherAggregateId(), anonymizeEvent.getStudentAggregateId(),
                        anonymizeEvent.getName(), anonymizeEvent.getUsername(),
                        anonymizeEvent.getPublisherAggregateVersion());
                commandGateway.send(command);
                unitOfWorkService.commit(unitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void processRemoveCourseExecutionEvent(Integer aggregateId,
            DeleteCourseExecutionEvent deleteCourseExecutionEvent) {
        logger.info("Processing RemoveCourseExecutionEvent: aggregateId={}, event={}", aggregateId,
                deleteCourseExecutionEvent);
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        var command = new RemoveCourseExecutionCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                aggregateId, deleteCourseExecutionEvent.getPublisherAggregateId(),
                deleteCourseExecutionEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processUpdateTopicEvent(Integer aggregateId, UpdateTopicEvent updateTopicEvent) {
        logger.info("Processing UpdateTopicEvent: aggregateId={}, event={}", aggregateId, updateTopicEvent);
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        var command = new UpdateTopicCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), aggregateId,
                updateTopicEvent.getPublisherAggregateId(), updateTopicEvent.getTopicName(),
                updateTopicEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processDeleteTopicEvent(Integer aggregateId, DeleteTopicEvent deleteTopicEvent) {
        logger.info("Processing DeleteTopicEvent: aggregateId={}, event={}", aggregateId, deleteTopicEvent);
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        var command = new RemoveTopicCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), aggregateId,
                deleteTopicEvent.getPublisherAggregateId(), deleteTopicEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processAnswerQuestionEvent(Integer aggregateId,
            QuizAnswerQuestionAnswerEvent quizAnswerQuestionAnswerEvent) {
        logger.info("Processing QuizAnswerQuestionAnswerEvent: aggregateId={}, event={}", aggregateId,
                quizAnswerQuestionAnswerEvent);
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        var command = new UpdateParticipantAnswerCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                aggregateId, quizAnswerQuestionAnswerEvent.getStudentAggregateId(),
                quizAnswerQuestionAnswerEvent.getPublisherAggregateId(),
                quizAnswerQuestionAnswerEvent.getQuestionAggregateId(), quizAnswerQuestionAnswerEvent.isCorrect(),
                quizAnswerQuestionAnswerEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processDisenrollStudentFromCourseExecutionEvent(Integer aggregateId,
            DisenrollStudentFromCourseExecutionEvent disenrollStudentFromCourseExecutionEvent) {
        logger.info("Processing DisenrollStudentFromCourseExecutionEvent: aggregateId={}, event={}", aggregateId,
                disenrollStudentFromCourseExecutionEvent);
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        var command = new RemoveUserCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), aggregateId,
                disenrollStudentFromCourseExecutionEvent.getPublisherAggregateId(),
                disenrollStudentFromCourseExecutionEvent.getStudentAggregateId(),
                disenrollStudentFromCourseExecutionEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processInvalidateQuizEvent(Integer aggregateId, InvalidateQuizEvent invalidateQuizEvent) {
        logger.info("Processing InvalidateQuizEvent: aggregateId={}, event={}", aggregateId, invalidateQuizEvent);
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        var command = new InvalidateQuizCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), aggregateId,
                invalidateQuizEvent.getPublisherAggregateId(), invalidateQuizEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processUpdateStudentNameEvent(Integer subscriberAggregateId,
            UpdateStudentNameEvent updateStudentNameEvent) {
        logger.info("Processing UpdateStudentNameEvent: subscriberAggregateId={}, event={}", subscriberAggregateId,
                updateStudentNameEvent);
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateUserNameFunctionalitySagas updateUserNameFunctionalitySagas = new UpdateUserNameFunctionalitySagas(
                        sagaUnitOfWorkService, updateStudentNameEvent.getPublisherAggregateVersion(),
                        subscriberAggregateId, updateStudentNameEvent.getPublisherAggregateId(),
                        updateStudentNameEvent.getStudentAggregateId(), sagaUnitOfWork,
                        updateStudentNameEvent.getUpdatedName(), commandGateway);
                updateUserNameFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                UnitOfWork unitOfWork = unitOfWorkService
                        .createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
                var command = new UpdateUserNameCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                        subscriberAggregateId, updateStudentNameEvent.getPublisherAggregateId(),
                        updateStudentNameEvent.getPublisherAggregateVersion(),
                        updateStudentNameEvent.getStudentAggregateId(), updateStudentNameEvent.getUpdatedName());
                commandGateway.send(command);
                unitOfWorkService.commit(unitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }
}
