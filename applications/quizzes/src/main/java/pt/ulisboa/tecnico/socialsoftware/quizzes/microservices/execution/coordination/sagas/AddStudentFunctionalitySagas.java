package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.EnrollStudentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.util.ArrayList;
import java.util.Arrays;

public class AddStudentFunctionalitySagas extends WorkflowFunctionality {

    private UserDto userDto;
    private CourseExecutionDto courseExecution;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddStudentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                        Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork,
                                        CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetUserByIdCommand getUserByIdCommand = new GetUserByIdCommand(unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            UserDto user = (UserDto) commandGateway.send(getUserByIdCommand);
            setUserDto(user);
        });

        SagaStep enrollStudentStep = new SagaStep("enrollStudentStep", () -> {
            EnrollStudentCommand enrollStudentCommand = new EnrollStudentCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId, this.getUserDto());
            CourseExecutionDto courseExecution = (CourseExecutionDto) commandGateway.send(enrollStudentCommand);
            this.setCourseExecution(courseExecution);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        workflow.addStep(getUserStep);
        workflow.addStep(enrollStudentStep);
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public CourseExecutionDto getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(CourseExecutionDto courseExecution) {
        this.courseExecution = courseExecution;
    }
}