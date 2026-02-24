package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.CancelTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;

public class CancelTournamentFunctionalityTCC extends WorkflowFunctionality {
    private Tournament oldTournament;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CancelTournamentFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                            Integer tournamentAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            CancelTournamentCommand cancelTournamentCommand = new CancelTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            commandGateway.send(cancelTournamentCommand);
        });

        workflow.addStep(step);
    }

    public Tournament getOldTournament() {
        return oldTournament;
    }

    public void setOldTournament(Tournament oldTournament) {
        this.oldTournament = oldTournament;
    }
}