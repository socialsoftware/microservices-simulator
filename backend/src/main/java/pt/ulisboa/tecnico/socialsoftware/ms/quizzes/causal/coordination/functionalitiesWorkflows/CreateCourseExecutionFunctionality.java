package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalitiesWorkflows;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;

public class CreateCourseExecutionFunctionality extends WorkflowFunctionality {
    private CourseExecutionDto courseExecutionDto;
    private CourseExecutionDto createdCourseExecution;

    private CausalWorkflow workflow;

    private final CourseExecutionService courseExecutionService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public CreateCourseExecutionFunctionality(CourseExecutionService courseExecutionService, CausalUnitOfWorkService unitOfWorkService, 
                                    CourseExecutionDto courseExecutionDto, CausalUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseExecutionDto, unitOfWork);
    }

    public void buildWorkflow(CourseExecutionDto courseExecutionDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.courseExecutionDto = courseExecutionService.createCourseExecution(courseExecutionDto, unitOfWork);
        });
    
        workflow.addStep(step);
    }

    @Override
    public void handleEvents() {

    }

    public void executeWorkflow(CausalUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, CausalUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, CausalUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(CausalUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
    }

    public CourseExecutionDto getCourseExecutionDto() {
        return courseExecutionDto;
    }

    public void setCourseExecutionDto(CourseExecutionDto courseExecutionDto) {
        this.courseExecutionDto = courseExecutionDto;
    }

    public CourseExecutionDto getCreatedCourseExecution() {
        return createdCourseExecution;
    }

    public void setCreatedCourseExecution(CourseExecutionDto createdCourseExecution) {
        this.createdCourseExecution = createdCourseExecution;
    }
}

