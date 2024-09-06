package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalitiesWorkflows;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;

public class AnonymizeStudentFunctionality extends WorkflowFunctionality {
    private CourseExecution oldCourseExecution;

    private CausalWorkflow workflow;

    private final CourseExecutionService courseExecutionService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public AnonymizeStudentFunctionality(CourseExecutionService courseExecutionService, CausalUnitOfWorkService unitOfWorkService, CourseExecutionFactory courseExecutionFactory, 
                                    Integer executionAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, userAggregateId, courseExecutionFactory, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, CourseExecutionFactory courseExecutionFactory, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            courseExecutionService.anonymizeStudent(executionAggregateId, userAggregateId, unitOfWork);
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

    public CourseExecution getOldCourseExecution() {
        return oldCourseExecution;
    }

    public void setOldCourseExecution(CourseExecution oldCourseExecution) {
        this.oldCourseExecution = oldCourseExecution;
    }
}