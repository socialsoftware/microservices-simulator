package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AnonymizeStudentFunctionalitySagas extends WorkflowFunctionality {
    
    private CourseExecution oldCourseExecution;

    

    private final CourseExecutionService courseExecutionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AnonymizeStudentFunctionalitySagas(CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService, CourseExecutionFactory courseExecutionFactory, 
                                    Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, userAggregateId, courseExecutionFactory, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, CourseExecutionFactory courseExecutionFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getOldCourseExecutionStep = new SyncStep("getOldCourseExecutionStep", () -> {
            SagaCourseExecution oldCourseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldCourseExecution, CourseExecutionSagaState.ANONYMIZE_STUDENT_READ_COURSE, unitOfWork);
            this.setOldCourseExecution(oldCourseExecution);
        });
    
        getOldCourseExecutionStep.registerCompensation(() -> {
            CourseExecution newCourseExecution = courseExecutionFactory.createCourseExecutionFromExisting(this.getOldCourseExecution());
            unitOfWorkService.registerSagaState((SagaCourseExecution) newCourseExecution, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newCourseExecution);
        }, unitOfWork);
    
        SyncStep anonymizeStudentStep = new SyncStep("anonymizeStudentStep", () -> {
            courseExecutionService.anonymizeStudent(executionAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getOldCourseExecutionStep)));
    
        workflow.addStep(getOldCourseExecutionStep);
        workflow.addStep(anonymizeStudentStep);
    }

    @Override
    public void handleEvents() {

    }

    

    public CourseExecution getOldCourseExecution() {
        return oldCourseExecution;
    }

    public void setOldCourseExecution(CourseExecution oldCourseExecution) {
        this.oldCourseExecution = oldCourseExecution;
    }
}