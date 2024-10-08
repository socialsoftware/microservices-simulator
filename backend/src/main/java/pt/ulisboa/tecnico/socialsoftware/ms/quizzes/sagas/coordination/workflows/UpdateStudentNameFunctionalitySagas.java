package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.USER_MISSING_NAME;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaCourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateStudentNameFunctionalitySagas extends WorkflowFunctionality {
    
    private UserDto student;
    private CourseExecutionDto courseExecution;
        
    

    private final CourseExecutionService courseExecutionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CourseExecutionFactory courseExecutionFactory;

    public UpdateStudentNameFunctionalitySagas(CourseExecutionService courseExecutionService, CourseExecutionFactory courseExecutionFactory, SagaUnitOfWorkService unitOfWorkService, Integer executionAggregateId, Integer userAggregateId, UserDto userDto, SagaUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.courseExecutionFactory = courseExecutionFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, userAggregateId, userDto, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, UserDto userDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        if (userDto.getName() == null) {
            throw new TutorException(USER_MISSING_NAME);
        }
        
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

        SagaSyncStep getStudentStep = new SagaSyncStep("getStudentStep", () -> {
            UserDto student = courseExecutionService.getStudentByExecutionIdAndUserId(executionAggregateId, userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(executionAggregateId, CourseExecutionSagaState.READ_STUDENT, new ArrayList<>(Arrays.asList(CourseExecutionSagaState.READ_COURSE)), unitOfWork);
            this.setStudent(student);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));

        getStudentStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(executionAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep updateStudentNameStep = new SagaSyncStep("updateStudentNameStep", () -> {
            courseExecutionService.updateExecutionStudentName(executionAggregateId, userAggregateId, userDto.getName(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep,getStudentStep)));
    
        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(getStudentStep);
        workflow.addStep(updateStudentNameStep);
    }
    public UserDto getStudent() {
        return student;
    }

    public void setStudent(UserDto student) {
        this.student = student;
    }

    public CourseExecutionDto getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(CourseExecutionDto courseExecution) {
        this.courseExecution = courseExecution;
    }
}

