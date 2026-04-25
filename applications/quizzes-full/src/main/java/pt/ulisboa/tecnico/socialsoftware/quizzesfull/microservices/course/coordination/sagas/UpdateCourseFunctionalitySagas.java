package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.UpdateCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states.CourseSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateCourseFunctionalitySagas extends WorkflowFunctionality {
    private CourseDto courseDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateCourseFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                          Integer courseAggregateId, String name, String type,
                                          SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, name, type, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, String name, String type, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            GetCourseByIdCommand getCmd = new GetCourseByIdCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCmd);
            sagaCommand.setSemanticLock(CourseSagaState.IN_UPDATE_COURSE);
            this.courseDto = (CourseDto) commandGateway.send(sagaCommand);
        });

        getCourseStep.registerCompensation(() -> {
            Command releaseCmd = new Command(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            SagaCommand sagaCommand = new SagaCommand(releaseCmd);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        // Service throws COURSE_FIELDS_IMMUTABLE — name and type are P1 final fields
        SagaStep updateCourseStep = new SagaStep("updateCourseStep", () -> {
            UpdateCourseCommand cmd = new UpdateCourseCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId, name, type);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getCourseStep)));

        workflow.addStep(getCourseStep);
        workflow.addStep(updateCourseStep);
    }

    public CourseDto getCourseDto() {
        return courseDto;
    }
}
