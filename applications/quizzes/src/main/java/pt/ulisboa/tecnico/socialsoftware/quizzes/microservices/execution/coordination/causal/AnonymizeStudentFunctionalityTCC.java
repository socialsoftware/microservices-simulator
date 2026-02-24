package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.AnonymizeStudentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.Execution;

public class AnonymizeStudentFunctionalityTCC extends WorkflowFunctionality {
    private Execution oldExecution;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AnonymizeStudentFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                            Integer executionAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork,
                                            CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId,
                              CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            AnonymizeStudentCommand anonymizeStudentCommand = new AnonymizeStudentCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId, userAggregateId);
            commandGateway.send(anonymizeStudentCommand);
        });

        workflow.addStep(step);
    }

    public Execution getOldCourseExecution() {
        return oldExecution;
    }

    public void setOldCourseExecution(Execution oldExecution) {
        this.oldExecution = oldExecution;
    }
}