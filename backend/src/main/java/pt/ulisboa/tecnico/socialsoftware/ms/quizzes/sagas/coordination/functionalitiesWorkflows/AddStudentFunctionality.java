package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddStudentFunctionality extends WorkflowFunctionality {
    private UserDto userDto;
    private CourseExecution oldCourseExecution;

    private SagaWorkflow workflow;

    private final CourseExecutionService courseExecutionService;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddStudentFunctionality(CourseExecutionService courseExecutionService, UserService userService, SagaUnitOfWorkService unitOfWorkService, CourseExecutionFactory courseExecutionFactory, 
                                    Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, userAggregateId, courseExecutionFactory, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, CourseExecutionFactory courseExecutionFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getUserStep = new SyncStep(() -> {
            UserDto userDto = userService.getUserById(userAggregateId, unitOfWork);
            this.setUserDto(userDto);
        });
    
        SyncStep getOldCourseExecutionStep = new SyncStep(() -> {
            SagaCourseExecution oldCourseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldCourseExecution, SagaState.ADD_STUDENT_READ_COURSE, unitOfWork);
            this.setOldCourseExecution(oldCourseExecution);
        });
    
        getOldCourseExecutionStep.registerCompensation(() -> {
            CourseExecution newCourseExecution = courseExecutionFactory.createCourseExecutionFromExisting(this.getOldCourseExecution());
            unitOfWorkService.registerSagaState((SagaCourseExecution) newCourseExecution, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newCourseExecution);
        }, unitOfWork);
    
        SyncStep enrollStudentStep = new SyncStep(() -> {
            courseExecutionService.enrollStudent(executionAggregateId, this.getUserDto(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getUserStep, getOldCourseExecutionStep)));
    
        workflow.addStep(getUserStep);
        workflow.addStep(getOldCourseExecutionStep);
        workflow.addStep(enrollStudentStep);
    }

    @Override
    public void handleEvents() {

    }

    public void executeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public CourseExecution getOldCourseExecution() {
        return oldCourseExecution;
    }

    public void setOldCourseExecution(CourseExecution oldCourseExecution) {
        this.oldCourseExecution = oldCourseExecution;
    }
}