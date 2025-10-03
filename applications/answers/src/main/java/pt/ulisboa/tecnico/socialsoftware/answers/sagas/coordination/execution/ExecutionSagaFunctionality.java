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


}