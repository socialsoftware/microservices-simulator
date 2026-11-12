package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.CreateExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.DeleteExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.DisenrollStudentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.EnrollStudentInExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.GetExecutionByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.GetExecutionsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.GetUserExecutionsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.UpdateExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.service.ExecutionService;

import java.util.List;

@Service
public class ExecutionFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;
    @Autowired
    private ExecutionService executionService;

    public ExecutionDto getExecutionById(Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getExecutionById");
        GetExecutionByIdFunctionalitySagas saga = new GetExecutionByIdFunctionalitySagas(
                unitOfWorkService, executionAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getExecutionDto();
    }

    public List<ExecutionDto> getExecutions() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getExecutions");
        GetExecutionsFunctionalitySagas saga = new GetExecutionsFunctionalitySagas(
                unitOfWorkService, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getExecutions();
    }

    public List<ExecutionDto> getUserExecutions(Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getUserExecutions");
        GetUserExecutionsFunctionalitySagas saga = new GetUserExecutionsFunctionalitySagas(
                unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getExecutions();
    }

    public ExecutionDto createExecution(ExecutionDto executionDto) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createExecution");
        CreateExecutionFunctionalitySagas saga = new CreateExecutionFunctionalitySagas(
                unitOfWorkService, executionDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getExecutionDto();
    }

    public void updateExecution(Integer executionAggregateId, String acronym, String academicTerm) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateExecution");
        UpdateExecutionFunctionalitySagas saga = new UpdateExecutionFunctionalitySagas(
                unitOfWorkService, executionAggregateId, acronym, academicTerm, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void enrollStudentInExecution(Integer executionAggregateId, Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("enrollStudentInExecution");
        EnrollStudentInExecutionFunctionalitySagas saga = new EnrollStudentInExecutionFunctionalitySagas(
                unitOfWorkService, executionAggregateId, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void disenrollStudent(Integer executionAggregateId, Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("disenrollStudent");
        DisenrollStudentFunctionalitySagas saga = new DisenrollStudentFunctionalitySagas(
                unitOfWorkService, executionAggregateId, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteExecution(Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("deleteExecution");
        DeleteExecutionFunctionalitySagas saga = new DeleteExecutionFunctionalitySagas(
                unitOfWorkService, executionAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void setStudentActiveByEvent(Integer executionAggregateId, Integer userAggregateId, Boolean active,
                                        Long userVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("setStudentActiveByEvent");
        if (isInSaga(executionAggregateId, unitOfWork)) {
            return;
        }
        executionService.setStudentActive(executionAggregateId, userAggregateId, active, userVersion, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void setStudentNameByEvent(Integer executionAggregateId, Integer userAggregateId, String userName,
                                      Long userVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("setStudentNameByEvent");
        if (isInSaga(executionAggregateId, unitOfWork)) {
            return;
        }
        executionService.setStudentName(executionAggregateId, userAggregateId, userName, userVersion, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void anonymizeStudentByEvent(Integer executionAggregateId, Integer userAggregateId, String userName,
                                        String userUsername, Long userVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("anonymizeStudentByEvent");
        if (isInSaga(executionAggregateId, unitOfWork)) {
            return;
        }
        executionService.anonymizeStudent(executionAggregateId, userAggregateId, userName, userUsername,
                userVersion, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeDeletedStudentByEvent(Integer executionAggregateId, Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeDeletedStudentByEvent");
        if (isInSaga(executionAggregateId, unitOfWork)) {
            return;
        }
        executionService.removeDeletedStudent(executionAggregateId, userAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    // The guard belongs here rather than in the service methods, which the saga steps also call.
    private boolean isInSaga(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        SagaAggregate execution = (SagaAggregate) unitOfWorkService.aggregateLoadAndRegisterRead(
                executionAggregateId, unitOfWork);
        return !GenericSagaState.NOT_IN_SAGA.equals(execution.getSagaState());
    }
}
