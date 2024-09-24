package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    public enum State implements SagaState {
        REMOVE_COURSE_EXECUTION_READ_COURSE {
            @Override
            public String getStateName() {
                return "REMOVE_COURSE_EXECUTION_READ_COURSE";
            }
        }
    }

    private SagaCourseExecution courseExecution;

    

    private final CourseExecutionService courseExecutionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public RemoveCourseExecutionFunctionalitySagas(CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService, 
                                    Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getCourseExecutionStep = new SyncStep("getCourseExecutionStep", () -> {
            SagaCourseExecution courseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(courseExecution, State.REMOVE_COURSE_EXECUTION_READ_COURSE, unitOfWork);
            this.setCourseExecution(courseExecution);
        });
    
        getCourseExecutionStep.registerCompensation(() -> {
            SagaCourseExecution courseExecution = this.getCourseExecution();
            unitOfWorkService.registerSagaState(courseExecution, GenericSagaState.NOT_IN_SAGA, unitOfWork);
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

    public SagaCourseExecution getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(SagaCourseExecution courseExecution) {
        this.courseExecution = courseExecution;
    }
}