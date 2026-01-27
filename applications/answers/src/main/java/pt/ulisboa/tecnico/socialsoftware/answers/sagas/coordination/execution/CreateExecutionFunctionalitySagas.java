package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionUser;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateExecutionRequestDto;

public class CreateExecutionFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto createdExecutionDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateExecutionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ExecutionService executionService, CreateExecutionRequestDto createRequest) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateExecutionRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createExecutionStep = new SagaSyncStep("createExecutionStep", () -> {
            CourseDto courseDto = createRequest.getCourse();
            ExecutionCourse course = new ExecutionCourse(courseDto);
            Set<ExecutionUser> users = null;
            if (createRequest.getUsers() != null) {
                users = createRequest.getUsers().stream()
                    .map(ExecutionUser::new)
                    .collect(Collectors.toSet());
            }
            ExecutionDto createdExecutionDto = executionService.createExecution(course, createRequest, users, unitOfWork);
            setCreatedExecutionDto(createdExecutionDto);
        });

        workflow.addStep(createExecutionStep);

    }

    public ExecutionDto getCreatedExecutionDto() {
        return createdExecutionDto;
    }

    public void setCreatedExecutionDto(ExecutionDto createdExecutionDto) {
        this.createdExecutionDto = createdExecutionDto;
    }
}
