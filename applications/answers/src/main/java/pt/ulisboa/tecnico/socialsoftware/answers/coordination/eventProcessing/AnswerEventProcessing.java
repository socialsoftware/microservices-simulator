package pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.AnonymizeUserEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.DeleteExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.DisenrollStudentFromExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.UpdateStudentNameEvent;

@Service
public class AnswerEventProcessing {
    @Autowired
    private AnswerService answerService;
    
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        workflowType = Arrays.asList(activeProfiles).contains(SAGAS.getValue()) ? SAGAS : null;
        if (workflowType == null) {
            throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void processAnonymizeUserEvent(Integer aggregateId, AnonymizeUserEvent anonymizeUserEvent) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

        AnonymizeUserAnswerFunctionalitySagas anonymizeUserAnswerFunctionalitySagas =
                new AnonymizeUserAnswerFunctionalitySagas(answerService, sagaUnitOfWorkService, aggregateId, anonymizeUserEvent.getPublisherAggregateId(), anonymizeUserEvent.getStudentAggregateId(), anonymizeUserEvent.getName(), anonymizeUserEvent.getUsername(), anonymizeUserEvent.getPublisherAggregateVersion(), sagaUnitOfWork);
                
        anonymizeUserAnswerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
    }

    public void processInvalidateQuizEvent(Integer aggregateId, InvalidateQuizEvent invalidateQuizEvent) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

        InvalidateQuizAnswerFunctionalitySagas invalidateQuizAnswerFunctionalitySagas =
                new InvalidateQuizAnswerFunctionalitySagas(answerService, sagaUnitOfWorkService, aggregateId, invalidateQuizEvent.getPublisherAggregateId(), invalidateQuizEvent.getPublisherAggregateId(), invalidateQuizEvent.getPublisherAggregateVersion(), sagaUnitOfWork);
                
        invalidateQuizAnswerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
    }

    public void processDeleteExecutionEvent(Integer aggregateId, DeleteExecutionEvent deleteExecutionEvent) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

        DeleteExecutionAnswerFunctionalitySagas deleteExecutionAnswerFunctionalitySagas =
                new DeleteExecutionAnswerFunctionalitySagas(answerService, sagaUnitOfWorkService, aggregateId, deleteExecutionEvent.getPublisherAggregateId(), deleteExecutionEvent.getPublisherAggregateId(), deleteExecutionEvent.getPublisherAggregateVersion(), sagaUnitOfWork);
                
        deleteExecutionAnswerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
    }

    public void processDisenrollStudentFromExecutionEvent(Integer aggregateId, DisenrollStudentFromExecutionEvent disenrollStudentFromExecutionEvent) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

        DisenrollStudentFromExecutionAnswerFunctionalitySagas disenrollStudentFromExecutionAnswerFunctionalitySagas =
                new DisenrollStudentFromExecutionAnswerFunctionalitySagas(answerService, sagaUnitOfWorkService, aggregateId, disenrollStudentFromExecutionEvent.getPublisherAggregateId(), disenrollStudentFromExecutionEvent.getPublisherAggregateId(), disenrollStudentFromExecutionEvent.getPublisherAggregateVersion(), sagaUnitOfWork);
                
        disenrollStudentFromExecutionAnswerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
    }

    public void processUpdateStudentNameEvent(Integer aggregateId, UpdateStudentNameEvent updateStudentNameEvent) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

        UpdateStudentNameAnswerFunctionalitySagas updateStudentNameAnswerFunctionalitySagas =
                new UpdateStudentNameAnswerFunctionalitySagas(answerService, sagaUnitOfWorkService, aggregateId, updateStudentNameEvent.getPublisherAggregateId(), updateStudentNameEvent.getPublisherAggregateVersion(), aggregateId, updateStudentNameEvent.getPublisherAggregateId(), updateStudentNameEvent.getStudentAggregateId(), sagaUnitOfWork, updateStudentNameEvent.getUpdatedName());
                
        updateStudentNameAnswerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
    }
}