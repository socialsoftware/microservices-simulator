package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.EnrollStudentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

import java.util.ArrayList;
import java.util.Arrays;

public class AddStudentFunctionalitySagas extends WorkflowFunctionality {

    private UserDto userDto;
    private CourseExecutionDto courseExecution;
    private final CourseExecutionService courseExecutionService;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway CommandGateway;

    public AddStudentFunctionalitySagas(CourseExecutionService courseExecutionService, UserService userService,
            SagaUnitOfWorkService unitOfWorkService,
            Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork,
            CommandGateway CommandGateway) {
        this.courseExecutionService = courseExecutionService;
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(executionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            GetUserByIdCommand getUserByIdCommand = new GetUserByIdCommand(unitOfWork,
                    ServiceMapping.USER.getServiceName(), userAggregateId);
            UserDto user = (UserDto) CommandGateway.send(getUserByIdCommand);
            setUserDto(user);
        });

        SagaSyncStep enrollStudentStep = new SagaSyncStep("enrollStudentStep", () -> {
            EnrollStudentCommand enrollStudentCommand = new EnrollStudentCommand(unitOfWork,
                    ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId, this.getUserDto());
            CourseExecutionDto courseExecution = (CourseExecutionDto) CommandGateway.send(enrollStudentCommand);
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