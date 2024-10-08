package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;

public class GetCourseExecutionsFunctionalityTCC extends WorkflowFunctionality {
    private List<CourseExecutionDto> courseExecutions;
    private final CourseExecutionService courseExecutionService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public GetCourseExecutionsFunctionalityTCC(CourseExecutionService courseExecutionService, CausalUnitOfWorkService unitOfWorkService, 
                                    CausalUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.courseExecutions = courseExecutionService.getAllCourseExecutions(unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public List<CourseExecutionDto> getCourseExecutions() {
        return courseExecutions;
    }

    public void setCourseExecutions(List<CourseExecutionDto> courseExecutions) {
        this.courseExecutions = courseExecutions;
    }
}