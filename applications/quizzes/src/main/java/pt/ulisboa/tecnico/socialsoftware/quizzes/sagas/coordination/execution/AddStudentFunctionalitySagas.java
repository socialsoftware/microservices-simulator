package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SagasCommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.EnrollStudentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddStudentFunctionalitySagas extends WorkflowFunctionality {
    
    private UserDto userDto;
    private CourseExecutionDto courseExecution;
    private final CourseExecutionService courseExecutionService;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final SagasCommandGateway sagasCommandGateway;

    public AddStudentFunctionalitySagas(CourseExecutionService courseExecutionService, UserService userService, SagaUnitOfWorkService unitOfWorkService,
                                    Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork, SagasCommandGateway sagasCommandGateway) {
        this.courseExecutionService = courseExecutionService;
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.sagasCommandGateway = sagasCommandGateway;
        this.buildWorkflow(executionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            // SagaUserDto user = (SagaUserDto) userService.getUserById(userAggregateId, unitOfWork);
            GetUserByIdCommand getUserByIdCommand = new GetUserByIdCommand(unitOfWork, "userService", userAggregateId);
            SagaUserDto user = (SagaUserDto) sagasCommandGateway.send(getUserByIdCommand);
            setUserDto(user);
        });
    
        SagaSyncStep enrollStudentStep = new SagaSyncStep("enrollStudentStep", () -> {
            // courseExecutionService.enrollStudent(executionAggregateId, this.getUserDto(), unitOfWork);
            EnrollStudentCommand enrollStudentCommand = new EnrollStudentCommand(unitOfWork, "courseExecutionService", executionAggregateId, this.getUserDto());
            CourseExecutionDto courseExecution = (CourseExecutionDto) sagasCommandGateway.send(enrollStudentCommand);
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