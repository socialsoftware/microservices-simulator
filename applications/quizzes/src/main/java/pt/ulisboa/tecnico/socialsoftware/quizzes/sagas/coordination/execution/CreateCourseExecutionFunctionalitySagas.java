package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SagasCommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaCourseExecutionDto;

public class CreateCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private SagaCourseExecutionDto courseExecutionDto;
    private SagaCourseExecutionDto createdCourseExecution;
    private final CourseExecutionService courseExecutionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final SagasCommandGateway sagasCommandGateway;

    public CreateCourseExecutionFunctionalitySagas(CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService,
                                    CourseExecutionDto courseExecutionDto, SagaUnitOfWork unitOfWork, SagasCommandGateway sagasCommandGateway) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.sagasCommandGateway = sagasCommandGateway;
        this.buildWorkflow(courseExecutionDto, unitOfWork);
    }

    public void buildWorkflow(CourseExecutionDto courseExecutionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createCourseExecutionStep = new SagaSyncStep("createCourseExecutionStep", () -> {
            SagaCourseExecutionDto createdCourseExecution = (SagaCourseExecutionDto) courseExecutionService.createCourseExecution(courseExecutionDto, unitOfWork);
//            CreateCourseExecutionCommand createCourseExecutionCommand = new CreateCourseExecutionCommand(unitOfWork, "courseExecutionService", courseExecutionDto);
//            SagaCourseExecutionDto createdCourseExecution = (SagaCourseExecutionDto) sagasCommandGateway.send(createCourseExecutionCommand);
            this.setCreatedCourseExecution(createdCourseExecution);
        });

        workflow.addStep(createCourseExecutionStep);
    }
    public CourseExecutionDto getCourseExecutionDto() {
        return courseExecutionDto;
    }

    public void setCourseExecutionDto(SagaCourseExecutionDto courseExecutionDto) {
        this.courseExecutionDto = courseExecutionDto;
    }

    public CourseExecutionDto getCreatedCourseExecution() {
        return createdCourseExecution;
    }

    public void setCreatedCourseExecution(SagaCourseExecutionDto createdCourseExecution) {
        this.createdCourseExecution = createdCourseExecution;
    }
}

