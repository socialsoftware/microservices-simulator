package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaExecutionDto;

@Component
public class ExecutionSagaFunctionality extends WorkflowFunctionality {
private final ExecutionService executionService;
private final SagaUnitOfWorkService unitOfWorkService;

public ExecutionSagaFunctionality(ExecutionService executionService, SagaUnitOfWorkService
unitOfWorkService) {
this.executionService = executionService;
this.unitOfWorkService = unitOfWorkService;
}

    public Object createExecution(String name, String acronym, String academicTerm, LocalDateTime startDate, LocalDateTime endDate, Object course, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for createExecution
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object getExecutionById(Integer executionId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getExecutionById
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object getAllExecutions(SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getAllExecutions
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object getExecutionsByCourse(Integer courseId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getExecutionsByCourse
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object enrollStudent(Integer executionId, Integer studentId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for enrollStudent
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object unenrollStudent(Integer executionId, Integer studentId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for unenrollStudent
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object updateExecution(Integer executionId, String name, String acronym, String academicTerm, LocalDateTime startDate, LocalDateTime endDate, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for updateExecution
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object deleteExecution(Integer executionId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for deleteExecution
        // This method should orchestrate the saga workflow
        return null;
    }
}