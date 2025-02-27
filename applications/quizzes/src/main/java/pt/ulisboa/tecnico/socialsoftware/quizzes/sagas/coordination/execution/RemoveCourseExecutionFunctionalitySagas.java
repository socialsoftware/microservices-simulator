package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.execution;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaCourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveCourseExecutionFunctionalitySagas extends WorkflowFunctionality {


    private SagaCourseExecutionDto courseExecution;
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

        SagaSyncStep getCourseExecutionStep = new SagaSyncStep("getCourseExecutionStep", () -> {
            SagaCourseExecutionDto courseExecution = (SagaCourseExecutionDto) courseExecutionService.getCourseExecutionById(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(executionAggregateId, CourseExecutionSagaState.READ_COURSE, unitOfWork);
            this.setCourseExecution(courseExecution);
        });
    
        getCourseExecutionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(executionAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep removeCourseExecutionStep = new SagaSyncStep("removeCourseExecutionStep", () -> {
            courseExecutionService.removeCourseExecution(executionAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));
    
        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(removeCourseExecutionStep);
    }
    public SagaCourseExecutionDto getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(SagaCourseExecutionDto courseExecution) {
        this.courseExecution = courseExecution;
    }
}