package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.RemoveStudentFromCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecution;

public class RemoveStudentFromCourseExecutionFunctionalityTCC extends WorkflowFunctionality {
    private CourseExecution oldCourseExecution;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveStudentFromCourseExecutionFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                                            Integer courseExecutionAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork,
                                                            CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseExecutionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseExecutionAggregateId, Integer userAggregateId,
                              CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            RemoveStudentFromCourseExecutionCommand removeStudentFromCourseExecutionCommand = new RemoveStudentFromCourseExecutionCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), courseExecutionAggregateId, userAggregateId);
            commandGateway.send(removeStudentFromCourseExecutionCommand);
        });

        workflow.addStep(step);
    }

    public CourseExecution getOldCourseExecution() {
        return oldCourseExecution;
    }

    public void setOldCourseExecution(CourseExecution oldCourseExecution) {
        this.oldCourseExecution = oldCourseExecution;
    }
}