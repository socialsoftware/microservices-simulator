package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class SearchExecutionsFunctionalitySagas extends WorkflowFunctionality {
    private List<ExecutionDto> searchedExecutionDtos;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public SearchExecutionsFunctionalitySagas(ExecutionService executionService, SagaUnitOfWorkService unitOfWorkService, String acronym, String academicTerm, Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(acronym, academicTerm, courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(String acronym, String academicTerm, Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep searchExecutionsStep = new SagaSyncStep("searchExecutionsStep", () -> {
            List<ExecutionDto> searchedExecutionDtos = executionService.searchExecutions(acronym, academicTerm, courseAggregateId, unitOfWork);
            setSearchedExecutionDtos(searchedExecutionDtos);
        });

        workflow.addStep(searchExecutionsStep);
    }

    public List<ExecutionDto> getSearchedExecutionDtos() {
        return searchedExecutionDtos;
    }

    public void setSearchedExecutionDtos(List<ExecutionDto> searchedExecutionDtos) {
        this.searchedExecutionDtos = searchedExecutionDtos;
    }
}
