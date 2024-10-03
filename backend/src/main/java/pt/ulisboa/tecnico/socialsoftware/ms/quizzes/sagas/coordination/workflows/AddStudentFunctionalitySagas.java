package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaCourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.UserSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
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

    public AddStudentFunctionalitySagas(CourseExecutionService courseExecutionService, UserService userService, SagaUnitOfWorkService unitOfWorkService, CourseExecutionFactory courseExecutionFactory, 
                                    Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, userAggregateId, courseExecutionFactory, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, CourseExecutionFactory courseExecutionFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            SagaUserDto user = (SagaUserDto) userService.getUserById(userAggregateId, unitOfWork);
            if (user.getSagaState().equals(GenericSagaState.NOT_IN_SAGA)) {
                unitOfWorkService.registerSagaState(userAggregateId, UserSagaState.READ_USER, unitOfWork);
                this.setUserDto(user);
            }
            else {
                throw new TutorException(ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA);
            }
        });
    
        SagaSyncStep getCourseExecutionStep = new SagaSyncStep("getCourseExecutionStep", () -> {
            SagaCourseExecutionDto courseExecution = (SagaCourseExecutionDto) courseExecutionService.getCourseExecutionById(executionAggregateId, unitOfWork);
            if (courseExecution.getSagaState().equals(GenericSagaState.NOT_IN_SAGA)) {
                unitOfWorkService.registerSagaState(executionAggregateId, CourseExecutionSagaState.READ_COURSE, unitOfWork);
                this.setCourseExecution(courseExecution);
            }
            else {
                throw new TutorException(ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA);
            }
        });
    
        getCourseExecutionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(executionAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep enrollStudentStep = new SagaSyncStep("enrollStudentStep", () -> {
            courseExecutionService.enrollStudent(executionAggregateId, this.getUserDto(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getUserStep, getCourseExecutionStep)));
    
        workflow.addStep(getUserStep);
        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(enrollStudentStep);
    }

    @Override
    public void handleEvents() {

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