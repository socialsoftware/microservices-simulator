package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.AnonymizeStudentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.CreateExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.DeleteExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.DisenrollStudentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.EnrollStudentInExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.UpdateExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.UpdateStudentNameFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.service.ExecutionService;

@Service
public class ExecutionFunctionalities {

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private ExecutionService executionService;

    public ExecutionDto createExecution(String acronym, String academicTerm, Integer courseId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CreateExecutionFunctionalitySagas saga = new CreateExecutionFunctionalitySagas(
                unitOfWorkService, acronym, academicTerm, courseId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedExecutionDto();
    }

    public void updateExecution(Integer executionId, String acronym, String academicTerm) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        UpdateExecutionFunctionalitySagas saga = new UpdateExecutionFunctionalitySagas(
                unitOfWorkService, executionId, acronym, academicTerm, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteExecution(Integer executionId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        DeleteExecutionFunctionalitySagas saga = new DeleteExecutionFunctionalitySagas(
                unitOfWorkService, executionId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void enrollStudentInExecution(Integer executionId, Integer userId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        EnrollStudentInExecutionFunctionalitySagas saga = new EnrollStudentInExecutionFunctionalitySagas(
                unitOfWorkService, executionId, userId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void disenrollStudent(Integer executionId, Integer userId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        DisenrollStudentFunctionalitySagas saga = new DisenrollStudentFunctionalitySagas(
                unitOfWorkService, executionId, userId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void updateStudentName(Integer executionId, Integer userId, String name) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        UpdateStudentNameFunctionalitySagas saga = new UpdateStudentNameFunctionalitySagas(
                unitOfWorkService, executionId, userId, name, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void anonymizeStudent(Integer executionId, Integer userId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        AnonymizeStudentFunctionalitySagas saga = new AnonymizeStudentFunctionalitySagas(
                unitOfWorkService, executionId, userId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public ExecutionDto getExecutionById(Integer executionId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        return executionService.getExecutionById(executionId, unitOfWork);
    }
}
