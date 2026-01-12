package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.UpdateParticipantAnswerCommand;

public class UpdateParticipantAnswerFunctionalityTCC extends WorkflowFunctionality {
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateParticipantAnswerFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
            Integer tournamentAggregateId,
            Integer userAggregateId, Integer executionAggregateId, Integer questionAggregateId,
            boolean correct, Integer eventVersion, CausalUnitOfWork unitOfWork,
            CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(tournamentAggregateId, userAggregateId, executionAggregateId, questionAggregateId, correct,
                eventVersion, unitOfWork);
    }

    private void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, Integer executionAggregateId,
            Integer questionAggregateId, boolean correct, Integer eventVersion, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        Step step = new Step(() -> {
            UpdateParticipantAnswerCommand command = new UpdateParticipantAnswerCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(),
                    tournamentAggregateId, userAggregateId, executionAggregateId, questionAggregateId, correct,
                    eventVersion);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
