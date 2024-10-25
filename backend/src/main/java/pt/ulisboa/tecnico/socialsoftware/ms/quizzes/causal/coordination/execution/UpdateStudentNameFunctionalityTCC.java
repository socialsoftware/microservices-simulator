package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalUser;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

public class UpdateStudentNameFunctionalityTCC extends WorkflowFunctionality {
    private CausalUser student;
    private CourseExecution oldCourseExecution;
        
    

    private final CourseExecutionService courseExecutionService;
    private final CausalUnitOfWorkService unitOfWorkService;
    public UpdateStudentNameFunctionalityTCC(CourseExecutionService courseExecutionService, CourseExecutionFactory courseExecutionFactory, CausalUnitOfWorkService unitOfWorkService, Integer executionAggregateId, Integer userAggregateId, UserDto userDto, CausalUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, userAggregateId, userDto, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, UserDto userDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            courseExecutionService.updateExecutionStudentName(executionAggregateId, userAggregateId, userDto.getName(), unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public CausalUser getStudent() {
        return student;
    }

    public void setStudent(CausalUser student) {
        this.student = student;
    }

    public CourseExecution getOldCourseExecution() {
        return oldCourseExecution;
    }

    public void setOldCourseExecution(CourseExecution oldCourseExecution) {
        this.oldCourseExecution = oldCourseExecution;
    }
}

