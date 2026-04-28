package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.course.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.sagas.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto;

public class CreateCourseFunctionalitySagas extends WorkflowFunctionality {
    private CourseDto createdCourseDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateCourseFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateCourseRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateCourseRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createCourseStep = new SagaStep("createCourseStep", () -> {
            CreateCourseCommand cmd = new CreateCourseCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), createRequest);
            CourseDto createdCourseDto = (CourseDto) commandGateway.send(cmd);
            setCreatedCourseDto(createdCourseDto);
        });

        workflow.addStep(createCourseStep);
    }
    public CourseDto getCreatedCourseDto() {
        return createdCourseDto;
    }

    public void setCreatedCourseDto(CourseDto createdCourseDto) {
        this.createdCourseDto = createdCourseDto;
    }
}
