package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.execution;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaCourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AnonymizeStudentFunctionalitySagas extends WorkflowFunctionality {
    
    private SagaCourseExecutionDto courseExecution;
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
    
        SagaSyncStep anonymizeStudentStep = new SagaSyncStep("anonymizeStudentStep", () -> {
            courseExecutionService.anonymizeStudent(executionAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep)));
    
        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(anonymizeStudentStep);
    }
    

    public SagaCourseExecutionDto getCourseExecution() {
        return this.courseExecution;
    }

    public void setCourseExecution(SagaCourseExecutionDto courseExecution) {
        this.courseExecution = courseExecution;
    }
}