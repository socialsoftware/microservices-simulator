package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class FindParticipantFunctionalityTCC extends WorkflowFunctionality {
    private Tournament tournament;
    private UserDto participant;
    private final CausalUnitOfWorkService unitOfWorkService;

    public FindParticipantFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            UserDto participant = tournament.findParticipant(userAggregateId).buildDto();
            this.setParticipant(participant);
        });
    
        workflow.addStep(step);
    }
    

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Tournament getTournament() {
        return tournament;
    }


    public void setParticipant(UserDto participant) {
        this.participant = participant;
    }

    public UserDto getParticipant() {
        return this.participant;
    }
}