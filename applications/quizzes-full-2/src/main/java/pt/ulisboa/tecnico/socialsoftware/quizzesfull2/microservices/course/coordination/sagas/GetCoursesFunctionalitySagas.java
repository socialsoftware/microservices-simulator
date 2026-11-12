package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.course.GetCoursesCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto;

import java.util.List;

public class GetCoursesFunctionalitySagas extends WorkflowFunctionality {
    private List<CourseDto> courses;

    public GetCoursesFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                        SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, unitOfWork, commandGateway);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getCoursesStep = new SagaStep("getCoursesStep", () -> {
            GetCoursesCommand cmd = new GetCoursesCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName());
            this.courses = (List<CourseDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getCoursesStep);
    }

    public List<CourseDto> getCourses() {
        return courses;
    }
}
