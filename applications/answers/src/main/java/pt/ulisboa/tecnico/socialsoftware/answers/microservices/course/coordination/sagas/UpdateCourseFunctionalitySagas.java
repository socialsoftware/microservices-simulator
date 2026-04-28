package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.course.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.sagas.states.CourseSagaState;

public class UpdateCourseFunctionalitySagas extends WorkflowFunctionality {
    private CourseDto updatedCourseDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateCourseFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CourseDto courseDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseDto, unitOfWork);
    }

    public void buildWorkflow(CourseDto courseDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateCourseStep = new SagaStep("updateCourseStep", () -> {
            unitOfWorkService.verifySagaState(courseDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(CourseSagaState.READ_COURSE, CourseSagaState.UPDATE_COURSE, CourseSagaState.DELETE_COURSE)));
            unitOfWorkService.registerSagaState(courseDto.getAggregateId(), CourseSagaState.UPDATE_COURSE, unitOfWork);
            UpdateCourseCommand cmd = new UpdateCourseCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseDto);
            CourseDto updatedCourseDto = (CourseDto) commandGateway.send(cmd);
            setUpdatedCourseDto(updatedCourseDto);
        });

        workflow.addStep(updateCourseStep);
    }
    public CourseDto getUpdatedCourseDto() {
        return updatedCourseDto;
    }

    public void setUpdatedCourseDto(CourseDto updatedCourseDto) {
        this.updatedCourseDto = updatedCourseDto;
    }
}
