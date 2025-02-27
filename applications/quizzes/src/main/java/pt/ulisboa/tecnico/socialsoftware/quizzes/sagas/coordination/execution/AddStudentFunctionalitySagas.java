package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaCourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddStudentFunctionalitySagas extends WorkflowFunctionality {
    
    private UserDto userDto;
    private SagaCourseExecutionDto courseExecution;
    private final CourseExecutionService courseExecutionService;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddStudentFunctionalitySagas(CourseExecutionService courseExecutionService, UserService userService, SagaUnitOfWorkService unitOfWorkService,
                                    Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            SagaUserDto user = (SagaUserDto) userService.getUserById(userAggregateId, unitOfWork);
            setUserDto(user);
        });
    
        SagaSyncStep enrollStudentStep = new SagaSyncStep("enrollStudentStep", () -> {
            courseExecutionService.enrollStudent(executionAggregateId, this.getUserDto(), unitOfWork);
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

    public SagaCourseExecutionDto getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(SagaCourseExecutionDto courseExecution) {
        this.courseExecution = courseExecution;
    }
}