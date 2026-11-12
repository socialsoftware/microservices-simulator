package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.CreateExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;

import java.util.ArrayList;
import java.util.Arrays;

public class CreateExecutionFunctionalitySagas extends WorkflowFunctionality {
    private CourseDto courseDto;
    private ExecutionDto createdExecutionDto;

    public CreateExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, ExecutionDto executionDto,
                                             SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, executionDto, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, ExecutionDto executionDto,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // P4a prerequisite: the fetch itself is the check — it throws when the course does not exist,
        // so no explicit guard is written in ExecutionService. The fetched DTO seeds the
        // courseAggregateId, courseName and courseType snapshot fields of the new Execution.
        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            GetCourseByIdCommand cmd = new GetCourseByIdCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), executionDto.getCourseAggregateId());
            this.courseDto = (CourseDto) commandGateway.send(cmd);
        });

        SagaStep createExecutionStep = new SagaStep("createExecutionStep", () -> {
            CreateExecutionCommand cmd = new CreateExecutionCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionDto, this.courseDto);
            this.createdExecutionDto = (ExecutionDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getCourseStep)));

        this.workflow.addStep(getCourseStep);
        this.workflow.addStep(createExecutionStep);
    }

    public ExecutionDto getExecutionDto() {
        return createdExecutionDto;
    }
}
