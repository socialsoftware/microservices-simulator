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
import java.util.stream.Collectors;

public class CreateExecutionFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto createdExecutionDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private SagaCourseDto courseDto;
    private ExecutionCourse course;
    private final CourseService courseService;


    public CreateExecutionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ExecutionService executionService, CourseService courseService, ExecutionDto executionDto) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.courseService = courseService;
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
            Set<ExecutionUser> users = executionDto.getUsers() != null ? executionDto.getUsers().stream().map(ExecutionUser::new).collect(java.util.stream.Collectors.toSet()) : null;
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
