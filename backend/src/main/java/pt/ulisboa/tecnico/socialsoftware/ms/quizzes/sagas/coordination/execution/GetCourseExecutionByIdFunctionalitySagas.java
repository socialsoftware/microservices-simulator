package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetCourseExecutionByIdFunctionalitySagas extends WorkflowFunctionality {
    private CourseExecutionDto courseExecutionDto;
    private final CourseExecutionService courseExecutionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetCourseExecutionByIdFunctionalitySagas(CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService, 
                                        Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseExecutionStep = new SagaSyncStep("getCourseExecutionStep", () -> {
            CourseExecutionDto courseExecutionDto = courseExecutionService.getCourseExecutionById(executionAggregateId, unitOfWork);
            this.setCourseExecutionDto(courseExecutionDto);
        });
    
        workflow.addStep(getCourseExecutionStep);
    }
    

    public CourseExecutionDto getCourseExecutionDto() {
        return courseExecutionDto;
    }

    public void setCourseExecutionDto(CourseExecutionDto courseExecutionDto) {
        this.courseExecutionDto = courseExecutionDto;
    }
}
