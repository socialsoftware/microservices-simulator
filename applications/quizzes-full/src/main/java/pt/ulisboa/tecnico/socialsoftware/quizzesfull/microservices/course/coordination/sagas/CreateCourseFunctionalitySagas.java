package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.CreateCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;

public class CreateCourseFunctionalitySagas extends WorkflowFunctionality {
    private CourseDto createdCourseDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateCourseFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                          String name, String type,
                                          SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(name, type, unitOfWork);
    }

    public void buildWorkflow(String name, String type, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createCourseStep = new SagaStep("createCourseStep", () -> {
            CreateCourseCommand cmd = new CreateCourseCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), name, type);
            this.createdCourseDto = (CourseDto) commandGateway.send(cmd);
        });

        workflow.addStep(createCourseStep);
    }

    public CourseDto getCreatedCourseDto() {
        return createdCourseDto;
    }
}
