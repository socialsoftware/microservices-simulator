package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveCourseExecutionFunctionality extends WorkflowFunctionality {
    private SagaCourseExecution courseExecution;

    private SagaWorkflow workflow;

    private final CourseExecutionService courseExecutionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public RemoveCourseExecutionFunctionality(CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService, 
                                    Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getCourseExecutionStep = new SyncStep("getCourseExecutionStep", () -> {
            SagaCourseExecution courseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(courseExecution, SagaState.REMOVE_COURSE_EXECUTION_READ_COURSE, unitOfWork);
            this.setCourseExecution(courseExecution);
        });
    
        getCourseExecutionStep.registerCompensation(() -> {
            SagaCourseExecution courseExecution = this.getCourseExecution();
            unitOfWorkService.registerSagaState(courseExecution, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(courseExecution);
        }, unitOfWork);
    
        SyncStep removeCourseExecutionStep = new SyncStep("removeCourseExecutionStep", () -> {
            courseExecutionService.removeCourseExecution(executionAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));
    
        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(removeCourseExecutionStep);
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

    public SagaCourseExecution getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(SagaCourseExecution courseExecution) {
        this.courseExecution = courseExecution;
    }
}