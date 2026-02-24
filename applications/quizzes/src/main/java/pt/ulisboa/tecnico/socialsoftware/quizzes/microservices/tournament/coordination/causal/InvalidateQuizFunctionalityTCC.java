package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.InvalidateQuizCommand;

public class InvalidateQuizFunctionalityTCC extends WorkflowFunctionality {
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public InvalidateQuizFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId,
            Integer quizAggregateId, Integer eventVersion, CausalUnitOfWork unitOfWork,
            CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(tournamentAggregateId, quizAggregateId, eventVersion, unitOfWork);
    }

    private void buildWorkflow(Integer tournamentAggregateId, Integer quizAggregateId, Integer eventVersion,
            CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        Step step = new Step(() -> {
            InvalidateQuizCommand command = new InvalidateQuizCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(),
                    tournamentAggregateId, quizAggregateId, eventVersion);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
