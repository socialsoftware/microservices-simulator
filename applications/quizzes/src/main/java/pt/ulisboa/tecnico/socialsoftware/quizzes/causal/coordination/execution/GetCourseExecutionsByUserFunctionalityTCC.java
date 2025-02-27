package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution;

import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;

public class GetCourseExecutionsByUserFunctionalityTCC extends WorkflowFunctionality {
    private Set<CourseExecutionDto> courseExecutions;
    private final CourseExecutionService courseExecutionService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public GetCourseExecutionsByUserFunctionalityTCC(CourseExecutionService courseExecutionService, CausalUnitOfWorkService unitOfWorkService, 
                                    Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.courseExecutions = courseExecutionService.getCourseExecutionsByUserId(userAggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public Set<CourseExecutionDto> getCourseExecutions() {
        return courseExecutions;
    }

    public void setCourseExecutions(Set<CourseExecutionDto> courseExecutions) {
        this.courseExecutions = courseExecutions;
    }
}