package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import ${this.getBasePackage()}.ms.coordination.workflow.WorkflowFunctionality;
import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWork;
import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import ${this.getBasePackage()}.ms.sagas.workflow.SagaSyncStep;
import ${this.getBasePackage()}.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.TournamentSagaState;
import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;

public class TournamentSagaCoordination extends WorkflowFunctionality {
private TournamentDto tournamentDto;
private SagaTournamentDto tournament;
private final TournamentService tournamentService;
private final SagaUnitOfWorkService unitOfWorkService;

public TournamentSagaCoordination(TournamentService tournamentService, SagaUnitOfWorkService
unitOfWorkService,
TournamentDto tournamentDto, SagaUnitOfWork unitOfWork) {
this.tournamentService = tournamentService;
this.unitOfWorkService = unitOfWorkService;
this.buildWorkflow(tournamentDto, unitOfWork);
}

public void buildWorkflow(TournamentDto tournamentDto, SagaUnitOfWork unitOfWork) {
this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
// Saga coordination logic will be implemented here
}

// Getters and setters
public TournamentDto getTournamentDto() {
return tournamentDto;
}

public void setTournamentDto(TournamentDto tournamentDto) {
this.tournamentDto = tournamentDto;
}

public SagaTournamentDto getTournament() {
return tournament;
}

public void setTournament(SagaTournamentDto tournament) {
this.tournament = tournament;
}
}