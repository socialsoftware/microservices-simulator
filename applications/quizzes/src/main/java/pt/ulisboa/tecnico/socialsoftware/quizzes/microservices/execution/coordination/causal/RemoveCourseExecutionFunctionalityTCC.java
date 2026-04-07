package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.UpdateCourseExecutionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.RemoveCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.causal.CausalExecution;

public class RemoveCourseExecutionFunctionalityTCC extends WorkflowFunctionality {
    private CausalExecution courseExecution;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveCourseExecutionFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                                 Integer executionAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            GetCourseExecutionByIdCommand getExecutionCmd = new GetCourseExecutionByIdCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            CourseExecutionDto executionDto = (CourseExecutionDto) commandGateway.send(getExecutionCmd);
            Integer courseAggregateId = executionDto.getCourseAggregateId();

            UpdateCourseExecutionCountCommand updateCmd = new UpdateCourseExecutionCountCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId, false);
            commandGateway.send(updateCmd);

            RemoveCourseExecutionCommand removeCourseExecutionCommand = new RemoveCourseExecutionCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            commandGateway.send(removeCourseExecutionCommand);
        });

        workflow.addStep(step);
    }

    public CausalExecution getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(CausalExecution courseExecution) {
        this.courseExecution = courseExecution;
    }
}
