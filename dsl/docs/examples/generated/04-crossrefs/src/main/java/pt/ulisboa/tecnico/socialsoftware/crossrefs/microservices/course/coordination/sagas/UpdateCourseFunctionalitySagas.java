package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.course.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

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
