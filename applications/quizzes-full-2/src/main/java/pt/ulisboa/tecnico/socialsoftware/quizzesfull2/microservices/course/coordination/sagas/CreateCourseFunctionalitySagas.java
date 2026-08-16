package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.course.CreateCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto;

public class CreateCourseFunctionalitySagas extends WorkflowFunctionality {
    private CourseDto courseDto;

    public CreateCourseFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CourseDto courseDto,
                                          SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, courseDto, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, CourseDto courseDto,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createCourseStep = new SagaStep("createCourseStep", () -> {
            CreateCourseCommand cmd = new CreateCourseCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), courseDto);
            this.courseDto = (CourseDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(createCourseStep);
    }

    public CourseDto getCourseDto() {
        return courseDto;
    }
}
