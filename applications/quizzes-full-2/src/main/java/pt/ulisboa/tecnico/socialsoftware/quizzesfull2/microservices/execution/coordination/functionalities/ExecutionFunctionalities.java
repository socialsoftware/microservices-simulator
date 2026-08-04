package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
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

import java.util.List;

@Service
public class ExecutionFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;

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
}
