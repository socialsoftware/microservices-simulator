package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionUser;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaUserDto;
import java.util.Set;
import java.util.HashSet;

public class CreateExecutionFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto createdExecutionDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private SagaCourseDto courseDto;
    private ExecutionCourse course;
    private final CourseService courseService;
    private final UserService userService;


    public CreateExecutionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ExecutionService executionService, CourseService courseService, UserService userService, ExecutionDto executionDto) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.courseService = courseService;
        this.userService = userService;
        this.buildWorkflow(executionDto, unitOfWork);
    }

    public void buildWorkflow(ExecutionDto executionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseStep = new SagaSyncStep("getCourseStep", () -> {
            Integer courseAggregateId = executionDto.getCourseAggregateId();
            courseDto = (SagaCourseDto) courseService.getCourseById(courseAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(courseDto.getAggregateId(), CourseSagaState.READ_COURSE, unitOfWork);
            ExecutionCourse course = new ExecutionCourse(courseDto);
            setCourse(course);
        });

        getCourseStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(courseDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep createExecutionStep = new SagaSyncStep("createExecutionStep", () -> {
            Set<ExecutionUser> users = null;
            if (executionDto.getUsersAggregateIds() != null) {
                users = new HashSet<>();
                for (Integer userAggregateId : executionDto.getUsersAggregateIds()) {
                    SagaUserDto userDto = (SagaUserDto) userService.getUserById(userAggregateId, unitOfWork);
                    users.add(new ExecutionUser(userDto));
                }
            }
            ExecutionDto createdExecutionDto = executionService.createExecution(getCourse(), executionDto, users, unitOfWork);
            setCreatedExecutionDto(createdExecutionDto);
        }, new ArrayList<>(Arrays.asList(getCourseStep)));

        workflow.addStep(getCourseStep);
        workflow.addStep(createExecutionStep);

    }

    public ExecutionCourse getCourse() {
        return course;
    }

    public void setCourse(ExecutionCourse course) {
        this.course = course;
    }

    public ExecutionDto getCreatedExecutionDto() {
        return createdExecutionDto;
    }

    public void setCreatedExecutionDto(ExecutionDto createdExecutionDto) {
        this.createdExecutionDto = createdExecutionDto;
    }
}
